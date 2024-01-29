package life.qbic.portal.portlet;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroup;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import life.qbic.datamodel.persons.Affiliation;
import life.qbic.datamodel.persons.CollaboratorWithResponsibility;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.utils.ConfigurationManager;
import life.qbic.portal.utils.ConfigurationManagerFactory;
import life.qbic.portal.utils.PortalUtils;
import life.qbic.userdb.Config;
import life.qbic.userdb.DBManager;
import life.qbic.userdb.model.Person;
import life.qbic.userdb.model.ProjectInfo;
import life.qbic.userdb.views.AffiliationInput;
import life.qbic.userdb.views.AffiliationVIPTab;
import life.qbic.userdb.views.MultiAffiliationTab;
import life.qbic.userdb.views.PersonBatchUpload;
import life.qbic.userdb.views.PersonInput;
import life.qbic.userdb.views.ProjectView;
import life.qbic.userdb.views.SearchView;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for portlet user-db-portlet. This class derives from {@link QBiCPortletUI}, which is
 * found in the {@code portal-utils-lib} library.
 */
@Theme("mytheme")
@Widgetset("life.qbic.portlet.AppWidgetSet")
public class UserDBPortletUI extends QBiCPortletUI {

  private static final Logger logger = LogManager.getLogger(UserDBPortletUI.class);
  private DBManager dbControl;
  private Map<String, Integer> affiMap;
  private Map<String, Person> personMap;
  private Map<String, ProjectInfo> projectMap;

  private TabSheet options;

  private Config config;
  public static String tmpFolder;

  private IOpenBisClient openbis;
  private final boolean development = false;

  @Override
  protected Layout getPortletContent(final VaadinRequest request) {
    final VerticalLayout layout = new VerticalLayout();
    layout.setMargin(true);
    setContent(layout);

    options = new TabSheet();

    String userID = "";
    boolean success = true;
    ConfigurationManager manager = ConfigurationManagerFactory.getInstance();
    tmpFolder = manager.getTmpFolder();
    if (PortalUtils.isLiferayPortlet()) {
      // read in the configuration file

      logger.info("User DB portlet is running on Liferay and user is logged in.");
      userID = PortalUtils.getUser().getScreenName();
    } else {
      if (development) {
        logger.warn("Checks for local dev version successful. User is granted admin status.");
        userID = "admin";
        // isAdmin = true;
      } else {
        success = false;
        logger.info(
            "User \"" + userID + "\" not found. Probably running on Liferay and not logged in.");
        layout.addComponent(new Label("User not found. Are you logged in?"));
      }
    }
    // establish connection to the OpenBIS API
    try {
      logger.debug("trying to connect to openbis");
      final String openbisURL = manager.getDataSourceUrl() + "/openbis/openbis";
      this.openbis = new OpenBisClient(manager.getDataSourceUser(), manager.getDataSourcePassword(),
          openbisURL);
      this.openbis.login();
    } catch (Exception e) {
      success = false;
      logger.error(
          "User \"" + userID + "\" could not connect to openBIS and has been informed of this.");
      layout.addComponent(new Label(
          "Data Management System could not be reached. Please try again later or contact us."));
    }
    if (success) {
      config = new Config(manager.getMysqlHost(), manager.getMysqlPort(), manager.getMysqlDB(),
          manager.getMysqlUser(), manager.getMysqlPass(), manager.getUserDBInputUserGrps(),
          manager.getUserDBInputAdminGrps(), manager.getDataSourceUrl(),
          manager.getDataSourceUser(), manager.getDataSourcePassword(), manager.getTmpFolder());
    }

    // LDAPConfig ldapConfig = readLdapConfig();// TODO

    dbControl = new DBManager(config);

    initTabs();

    layout.addComponent(options);
    return layout;
  }

  private void initTabs() {
    boolean admin = isAdmin();
    options.removeAllComponents();
    if (!admin && !development && !canUsePortlet()) {
      VerticalLayout rightsMissingTab = new VerticalLayout();
      rightsMissingTab.setCaption("User Database Input");
      Label info = new Label(
          "Your account does not have the necessary rights to add new people to our database.\n"
              + "If you think you should be able to do so, please contact us.",
          ContentMode.PREFORMATTED);
      rightsMissingTab.addComponent(info);
      options.addTab(rightsMissingTab, "Information");
      options.setSelectedTab(rightsMissingTab);
      options.setEnabled(false);
    } else {
      personMap = dbControl.getPersonMap();

      String userID = "";
      if (PortalUtils.isLiferayPortlet()) {
        logger.info("DB Tools running on Liferay, fetching user ID.");
        userID = PortalUtils.getUser().getScreenName();
      } else {
        if (development) {
          logger.warn("Checks for local dev version successful. User is granted admin status.");
          userID = "admin";
        }
      }
      Map<String, ProjectInfo> userProjects = new HashMap<>();

      List<Project> openbisProjectsForUser = new ArrayList<>();
      Set<String> spaces = new HashSet<>(openbis.getUserSpaces(userID));

      List<Project> allOpenbisProjects = openbis.listProjects();
      for (Project p : allOpenbisProjects) {
        String space = p.getSpace().getCode();
        if (spaces.contains(space)) {
          openbisProjectsForUser.add(p);
        }
      }

      Map<String, ProjectInfo> dbProjects = dbControl.getProjectMap();
      for (Project p : openbisProjectsForUser) {
        String desc = Objects.toString(p.getDescription(), "");
        desc = desc.replaceAll("\n+", ". ");
        String projectID = p.getIdentifier().getIdentifier();
        String code = p.getCode();
        if (dbProjects.get(projectID) == null)
          userProjects.put(projectID, new ProjectInfo(p.getSpace().getCode(), code, desc, "", -1));
        else {
          ProjectInfo info = dbProjects.get(projectID);
          info.setDescription(desc);
          userProjects.put(projectID, info);
        }
      }

      projectMap = new HashMap<>();
      for (ProjectInfo p : userProjects.values()) {
        String code = p.getProjectCode();
        projectMap.put(code, p);
      }
      ProjectView projectView = new ProjectView(projectMap, personMap);
      options.addTab(projectView, "Projects");
      options.getTab(projectView).setEnabled(!userProjects.isEmpty());

      // initPortletToDBFunctionality(addAffilTab, addUserTab, batchTab, multiAffilTab, vipTab,
      // searchView, projectView);
      projectView.getProjectTable().addValueChangeListener(new ValueChangeListener() {

        private final Map<String, String> expTypeCodeTranslation = new HashMap<String, String>() {
          {
            put("Q_EXPERIMENTAL_DESIGN", "Patients/Sources");
            put("Q_SAMPLE_EXTRACTION", "Sample Extracts");
            put("Q_SAMPLE_PREPARATION", "Sample Preparations");
            put("Q_MS_MEASUREMENT", "Mass Spectrometry");
            put("Q_NGS_MEASUREMENT", "NGS Sequencing");
          };
        };

        @Override
        public void valueChange(ValueChangeEvent event) {
          Object item = projectView.getProjectTable().getValue();
          if (item != null) {
            String project = item.toString();
            // get collaborators associated to openbis experiments
            List<CollaboratorWithResponsibility> collaborators =
                dbControl.getCollaboratorsOfProject(project);
            // get openbis experiments and type
            Map<String, String> existingExps = new HashMap<>();
            for (Experiment e : openbis.getExperimentsOfProjectByCode(project)) {
              String type = expTypeCodeTranslation.get(e.getType().getCode());
              String id = e.getIdentifier().getIdentifier();
              if (type != null)
                existingExps.put(id, type);
            }
            // add types for experiments with existing collaborators
            for (CollaboratorWithResponsibility c : collaborators) {
              String identifier = c.getOpenbisIdentifier();
              c.setType(existingExps.get(identifier));
              existingExps.remove(identifier);
            }
            // add empty entries and type for applicable experiments without collaborators
            for (String expID : existingExps.keySet()) {
              String code = expID.split("/")[3];
              CollaboratorWithResponsibility c =
                  new CollaboratorWithResponsibility(-1, "", expID, code, "Contact");
              c.setType(existingExps.get(expID));
              collaborators.add(c);
            }
            projectView.setCollaboratorsOfProject(collaborators);

            ProjectInfo info = projectMap.get(item);

            projectView.handleProjectValueChange(item, info.getInvestigator(), info.getContact(), info.getManager());
          } else {
            projectView.handleProjectDeselect();
          }
        }

      });

      projectView.getInfoCommitButton().addClickListener(new ClickListener() {

        @Override
        public void buttonClick(ClickEvent event) {
          ProjectInfo info = projectView.getEditedInfo();
          if (info != null) {
            String code = info.getProjectCode();
            int id = info.getProjectID();
            if (id < 1)
              id = dbControl.addProjectToDB("/" + info.getSpace() + "/" + code,
                  info.getSecondaryName());
            else
              dbControl.addOrChangeSecondaryNameForProject(id, info.getSecondaryName());
            if (info.getInvestigator() == null)
              dbControl.removePersonFromProject(id, "PI");
            else
              dbControl.addOrUpdatePersonToProject(id, info.getInvestigator().getId(), "PI");
            if (info.getContact() == null)
              dbControl.removePersonFromProject(id, "Contact");
            else
              dbControl.addOrUpdatePersonToProject(id, info.getContact().getId(), "Contact");
            if (info.getManager() == null)
              dbControl.removePersonFromProject(id, "Manager");
            else
              dbControl.addOrUpdatePersonToProject(id, info.getManager().getId(), "Manager");
            projectView.updateChangedInfo(info);
          }
        }
      });;
      projectView.getPeopleCommitButton().addClickListener(new ClickListener() {

        @Override
        public void buttonClick(ClickEvent event) {
          List<CollaboratorWithResponsibility> links = projectView.getNewResponsibilities();
          for (CollaboratorWithResponsibility c : links) {
            int experimentID = c.getExperimentID();
            if (experimentID < 1)
              experimentID = dbControl.addExperimentToDB(c.getOpenbisIdentifier());
            String name = c.getPerson();
            int personID = -1;
            if (personMap.get(name) != null)
              personID = personMap.get(name).getId();
            if (personID < 1)
              dbControl.removePersonFromExperiment(experimentID);
            else
              dbControl.addOrUpdatePersonToExperiment(experimentID, personID, "Contact");
          }
        }
      });;
    }
  }

  private Map<String, Integer> fillMaxInputLengthMap() {
    Map<String, Integer> res = new HashMap<>();
    try {
      res.putAll(dbControl.getColsMaxLengthsForTable("persons"));
      res.putAll(dbControl.getColsMaxLengthsForTable("organizations"));
    } catch (SQLException e) {
      logger.error(e.toString());
    }
    return res;
  }

  private boolean canUsePortlet() {
    try {
      User user = PortalUtils.getUser();
      for (UserGroup grp : user.getUserGroups()) {
        String group = grp.getName();
        if (config.getUserGrps().contains(group)) {
          logger.info("User " + user.getScreenName() + " can use portlet because they are part of "
              + group);
          return true;
        }
      }
    } catch (Exception e) {
      logger.error("Could not fetch user groups. User won't be able to use portlet.");
      logger.error(e.toString());
    }
    return false;
  }

  private boolean isAdmin() {
    if (development)
      return true;
    else {
      try {
        User user = PortalUtils.getUser();
        for (UserGroup grp : user.getUserGroups()) {
          String group = grp.getName();
          if (config.getAdminGrps().contains(group)) {
            logger.info("User " + user.getScreenName()
                + " has full rights because they are part of " + group);
            return true;
          }
        }
      } catch (Exception e) {
        logger.error("Could not fetch user groups. User won't be able to use portlet.");
        logger.error(e.toString());
      }
      return false;
    }
  }

  private void initPortletToDBFunctionality(final AffiliationInput addAffilTab,
      final PersonInput addUserTab, final PersonBatchUpload batchUpload,
      final MultiAffiliationTab multiAffilTab, final AffiliationVIPTab vipTab,
      final SearchView search, final ProjectView projects) {

    batchUpload.getRegisterButton().addClickListener(new Button.ClickListener() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      /**
       * 
       */

      @Override
      public void buttonClick(ClickEvent event) {
        if (batchUpload.isValid()) {
          List<String> registered = new ArrayList<>();
          batchUpload.setRegEnabled(false);
          for (Person p : batchUpload.getPeople()) {
            if (dbControl.personExists(p)) {
              registered.add(p.getFirstName() + " " + p.getLastName());
            } else {
              if (!dbControl.addNewPerson(p)) {
                commitError("There has been an error while adding a new person.");
              }
            }
          }
          if (registered.isEmpty())
            successfulCommit();
          else {
            Styles.notification("Person already registered", StringUtils.join(registered, ", ")
                + " had a username or email already registered in our database! They were skipped in the registration process.",
                NotificationType.DEFAULT);
          }
        }
      }
    });

    search.getSearchAffiliationButton().addClickListener(new Button.ClickListener() {
      @Override
      public void buttonClick(ClickEvent event) {
        String affi = search.getAffiliationSearchField().getValue();
        if (affi != null && !affi.isEmpty()) {
          search.setAffiliations(dbControl.getAffiliationsContaining(affi));
        } else
          search.setAffiliations(new ArrayList<>());
      }
    });

    search.getSearchPersonButton().addClickListener(new Button.ClickListener() {
      @Override
      public void buttonClick(ClickEvent event) {
        String personName = search.getPersonSearchField().getValue();
        if (personName != null && !personName.isEmpty()) {
          search.setPersons(dbControl.getPersonsContaining(personName));
        } else
          search.setPersons(new ArrayList<>());
      }
    });

    addAffilTab.getCommitButton().addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (addAffilTab.isValid()) {
          if (dbControl.addNewAffiliation(addAffilTab.getAffiliation()) > -1)
            successfulCommit();
          else
            commitError("There has been an error.");
        } else
          inputError();
      }
    });

    vipTab.getSetHeadAndContactButton().addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        int affi = vipTab.getSelectedAffiTabID();
        int contact = vipTab.getNewContactID();
        int head = vipTab.getNewHeadID();
        if (affi > 0) {
          if (head > 0) {
            dbControl.setAffiliationVIP(affi, head, "head");
          }
          if (contact > 0) {
            dbControl.setAffiliationVIP(affi, contact, "main_contact");
          }
          vipTab.updateVIPs();
          successfulCommit();
        }
      }
    });

    addAffilTab.getInstituteField().addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        Object val = addAffilTab.getInstituteField().getValue();
        if (val != null) {
          Affiliation orgInfo = dbControl.getOrganizationInfosFromInstitute(val.toString());
          if (orgInfo != null)
            addAffilTab.autoComplete(orgInfo);
        }
      }
    });

    addUserTab.getCommitButton().addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (addUserTab.isValid()) {
          Person p = addUserTab.getPerson();
          if (addUserTab.hasNewAffiliation()) {
            int affiID = dbControl.addNewAffiliation(addUserTab.getNewAffiliation());
            if (affiID > -1)
              successfulCommit();
            else
              commitError("There has been an error while adding the new affiliation.");
            p.setAffiliationID(affiID);
          }
          if (dbControl.personExists(p)) {
            Styles.notification("Person already registered",
                "A person with the Username or E-Mail you selected is already registered in our database!",
                NotificationType.ERROR);
          } else {
            if (dbControl.addNewPerson(p))
              successfulCommit();
            else
              commitError("There has been an error while adding a new person.");
          }
        } else
          inputError();
      }
    });

    multiAffilTab.getCommitButton().addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (multiAffilTab.isValid()) {
          if (dbControl.addOrUpdatePersonAffiliationConnections(
              personMap.get(multiAffilTab.getPersonBox().getValue()).getId(),
              multiAffilTab.getChangedAndNewConnections()))
            successfulCommit();
          else
            commitError("There has been an error.");
        } else
          inputError();
      }
    });

    multiAffilTab.getAddButton().addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        String personName = multiAffilTab.getPersonBox().getValue().toString();
        Person p = personMap.get(personName);

        String affiName = multiAffilTab.getOrganizationBox().getValue().toString();
        Person newP = new Person(p.getUsername(), p.getTitle(), p.getFirstName(), p.getLastName(),
            p.getEmail(), p.getPhone(), affiMap.get(affiName), affiName, "");
        multiAffilTab.addDataToTable(new ArrayList<>(Arrays.asList(newP)));
        multiAffilTab.getAddButton().setEnabled(false);
      }
    });

    ValueChangeListener multiAffiPersonListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (multiAffilTab.getPersonBox().getValue() != null) {
          String personName = multiAffilTab.getPersonBox().getValue().toString();
          multiAffilTab.reactToPersonSelection(personName,
              dbControl.getPersonWithAffiliations(personMap.get(personName).getId()));
          multiAffilTab.getAddButton().setEnabled(multiAffilTab.newAffiliationPossible());
        }
      }
    };
    multiAffilTab.getPersonBox().addValueChangeListener(multiAffiPersonListener);

    ValueChangeListener multiAffiListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (multiAffilTab.getPersonBox().getValue() != null) {
          multiAffilTab.getAddButton().setEnabled(multiAffilTab.newAffiliationPossible());
        }
      }
    };
    multiAffilTab.getOrganizationBox().addValueChangeListener(multiAffiListener);
  }

  private void successfulCommit() {
    Styles.notification("Data added", "Data has been successfully added to the database!",
        NotificationType.SUCCESS);
    // wait a bit and reload tabs
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      logger.error(e.toString());
    }
    initTabs();
  }

  private void inputError() {
    Styles.notification("Data Incomplete", "Please fill in all required fields correctly.",
        NotificationType.DEFAULT);
  }

  private void commitError(String reason) {
    Styles.notification("There has been an error.", reason, NotificationType.ERROR);
  }

}
