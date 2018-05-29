/*******************************************************************************
 * QBiC User DB Tools enables users to add people and affiliations to our mysql user database.
 * Copyright (C) 2016 Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.userdb.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tepi.filtertable.FilterTable;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.datamodel.persons.CollaboratorWithResponsibility;
import life.qbic.datamodel.projects.ProjectInfo;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.portal.Styles;
import life.qbic.portal.portlet.ProjectFilterDecorator;
import life.qbic.portal.portlet.ProjectFilterGenerator;

public class ProjectView extends VerticalLayout {
  Logger logger = LogManager.getLogger(ProjectView.class);

  private FilterTable projectTable;
  private Map<String, ProjectInfo> projectMap;
  private Map<String, Integer> personMap;

  private TextField altName;
  private ComboBox investigator;
  private ComboBox contact;
  private ComboBox manager;
  private Button submitInfo;

  private Table projectPersons;
  private Button submitPersons;
  private Map<String, CollaboratorWithResponsibility> experimentMap;

  public ProjectView(Collection<ProjectInfo> collection, IOpenBisClient openbis,
      Map<String, Integer> personMap) {
    setSpacing(true);
    setMargin(true);

    this.personMap = personMap;

    projectTable = new FilterTable("Projects");
    projectTable.setPageLength(Math.min(15, collection.size()));
    projectTable.setStyleName(ValoTheme.TABLE_SMALL);
    projectTable.addContainerProperty("Sub-Project", String.class, null);
    projectTable.addContainerProperty("Short Title", String.class, null);
    projectTable.setColumnWidth("Name", 300);
    projectTable.addContainerProperty("Project", String.class, null);
    projectTable.addContainerProperty("Principal Investigator", String.class, null);
    projectTable.setSelectable(true);
    addComponent(projectTable);

    projectTable.setFilterDecorator(new ProjectFilterDecorator());
    projectTable.setFilterGenerator(new ProjectFilterGenerator());

    projectTable.setFilterBarVisible(true);

    projectTable.setImmediate(true);

    initProjectInfos(collection);

    projectPersons = new Table("Experiment Collaborators (optional)");
    projectPersons.setStyleName(ValoTheme.TABLE_SMALL);
    projectPersons.addContainerProperty("Name", ComboBox.class, null);
    projectPersons.addContainerProperty("Experiment", String.class, null);
    projectPersons.addContainerProperty("Responsibility", String.class, null);
    projectPersons.setColumnWidth("Responsibility", 150);
    projectPersons.setPageLength(1);
    addComponent(projectPersons);

    submitPersons = new Button("Submit Experiment");
    addComponent(submitPersons);
  }

  public void initProjectInfos(Collection<ProjectInfo> collection) {
    projectMap = new HashMap<String, ProjectInfo>();
    projectTable.removeAllItems();
    for (ProjectInfo p : collection) {
      String code = p.getProjectCode();
      projectMap.put(code, p);
      List<Object> row = new ArrayList<Object>();
      row.add(code);
      row.add(p.getSecondaryName());
      row.add(p.getSpace());
      row.add(p.getInvestigator());
      projectTable.addItem(row.toArray(new Object[row.size()]), code);
    }

    // sort ascending by Project ID
    // projectTable.sort(new Object[] {"Sub-Project"}, new boolean[] {true});

    VerticalLayout projectInfo = new VerticalLayout();
    projectInfo.setVisible(false);
    altName = new TextField("Short Title");
    altName.setWidth("300px");
    altName.setStyleName(Styles.fieldTheme);
    investigator = new ComboBox("Principal Investigator", personMap.keySet());
    investigator.setStyleName(Styles.boxTheme);
    investigator.setFilteringMode(FilteringMode.CONTAINS);
    contact = new ComboBox("Contact Person", personMap.keySet());
    contact.setStyleName(Styles.boxTheme);
    contact.setFilteringMode(FilteringMode.CONTAINS);
    manager = new ComboBox("Project Manager", personMap.keySet());
    manager.setStyleName(Styles.boxTheme);
    manager.setFilteringMode(FilteringMode.CONTAINS);
    submitInfo = new Button("Change Project Information");
    projectInfo.addComponent(altName);
    projectInfo.addComponent(investigator);
    projectInfo.addComponent(contact);
    projectInfo.addComponent(manager);
    projectInfo.addComponent(submitInfo);
    projectInfo.setSpacing(true);
    addComponent(projectInfo);

    projectTable.addValueChangeListener(new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -3035074733968253748L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        projectInfo.setVisible(false);
        Object item = projectTable.getValue();
        if (item != null) {
          projectInfo.setVisible(true);
          projectInfo.setCaption(projectMap.get(item).getProjectCode());
          System.out.println(projectMap.get(item));
          altName.setValue(projectMap.get(item).getSecondaryName());
          investigator.setValue(projectMap.get(item).getInvestigator());
          contact.setValue(projectMap.get(item).getContact());
          manager.setValue(projectMap.get(item).getManager());
        }
      }
    });
  }

  /**
   * returns null if there was no change
   * 
   * @return
   */
  public ProjectInfo getEditedInfo() {
    String code = (String) projectTable.getValue();
    ProjectInfo p = projectMap.get(code);
    String oldName = p.getSecondaryName();
    String newName = altName.getValue();
    
    String oldPI = p.getInvestigator();
    Object newPI = investigator.getValue();
    boolean updatePI = oldPI != newPI;
    if (oldPI != null)
      updatePI = !oldPI.equals(newPI);

    String oldContact = p.getContact();
    Object newContact = contact.getValue();
    boolean updateContact = oldContact != newContact;
    if (oldContact != null)
      updateContact = !oldContact.equals(newContact);

    String oldManager = p.getManager();
    Object newManager = manager.getValue();
    boolean updateManager = oldManager != newManager;
    if (oldManager != null)
      updateManager = !oldManager.equals(newManager);
    
    boolean update = !oldName.equals(newName) || updatePI || updateContact || updateManager;
    if (update) {
      // initProjectInfos(projectMap.values());
      ProjectInfo newInfo = new ProjectInfo(p.getSpace(), code, newName, p.getProjectID());
      if (newPI != null)
        newInfo.setInvestigator(newPI.toString());
      else
        newInfo.setInvestigator("");
      if (newContact != null)
        newInfo.setContact(newContact.toString());
      else
        newInfo.setContact("");
      if (newManager != null)
        newInfo.setManager(newManager.toString());
      else
        newInfo.setManager("");
      return newInfo;
    } else {
      logger.debug("No changes to project info detected");
      return null;
    }
  }

  public Button getInfoCommitButton() {
    return submitInfo;
  }

  public Button getPeopleCommitButton() {
    return submitPersons;
  }

  public FilterTable getProjectTable() {
    return projectTable;
  }

  public Table getCollaboratorTable() {
    return projectPersons;
  }

  public void setCollaboratorsOfProject(List<CollaboratorWithResponsibility> collaborators) {
    experimentMap = new HashMap<String, CollaboratorWithResponsibility>();
    projectPersons.removeAllItems();

    for (CollaboratorWithResponsibility c : collaborators) {
      String code = c.getOpenbisCode();
      experimentMap.put(code, c);
      List<Object> row = new ArrayList<Object>();
      ComboBox persons = new ComboBox();
      persons.setStyleName(Styles.boxTheme);
      persons.setFilteringMode(FilteringMode.CONTAINS);
      persons.addItems(personMap.keySet());
      persons.setValue(c.getPerson());
      row.add(persons);
      row.add(c.getOpenbisCode());
      row.add(c.getOpenbisType());
      projectPersons.addItem(row.toArray(new Object[row.size()]), code);
    }

    // sort ascending by Experiment ID
    Object[] properties = {"Experiment"};
    boolean[] ordering = {true};
    projectPersons.sort(properties, ordering);

    projectPersons.sort();
    projectPersons.setPageLength(collaborators.size());
  }

  /**
   * 
   * @return list of new collaborators for experiments
   */
  public List<CollaboratorWithResponsibility> getNewResponsibilities() {
    List<CollaboratorWithResponsibility> res = new ArrayList<CollaboratorWithResponsibility>();
    for (Object code : projectPersons.getItemIds()) {
      ComboBox personBox =
          (ComboBox) projectPersons.getItem(code).getItemProperty("Name").getValue();
      String name = "";
      if (personBox.getValue() != null)
        name = personBox.getValue().toString();
      CollaboratorWithResponsibility old = experimentMap.get(code);
      CollaboratorWithResponsibility newColl = new CollaboratorWithResponsibility(
          old.getExperimentID(), name, old.getOpenbisIdentifier(), code.toString(), old.getRole());
      res.add(newColl);
    }
    return res;
  }

  public void updateChangedInfo(ProjectInfo info) {
    String code = info.getProjectCode();
    projectTable.removeItem(code);
    projectMap.put(code, info);
    List<Object> row = new ArrayList<Object>();
    row.add(code);
    row.add(info.getSecondaryName());
    row.add(info.getSpace());
    row.add(info.getInvestigator());
    projectTable.addItem(row.toArray(new Object[row.size()]), code);
    // sort ascending by Project ID
    projectTable.sort(new Object[] {"Sub-Project"}, new boolean[] {true});
  }

}
