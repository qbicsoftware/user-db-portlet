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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.datamodel.persons.Affiliation;
import life.qbic.portal.Styles;

public class AffiliationInput extends FormLayout {

  /**
   * 
   */
  private static final long serialVersionUID = 2499215556258217023L;
  private TextField groupName;
  private TextField acronym;
  private TextField organization;
  private ComboBox institute;
  private ComboBox faculty;
  private ComboBox contactPerson;
  private ComboBox head;
  private TextField street;
  private TextField zipCode;
  private TextField city;
  private TextField country;
  private TextField webpage;

  private Map<String, Integer> personMap;

  private Button commit;

  Logger logger = LogManager.getLogger(AffiliationInput.class);

  public AffiliationInput(Set<String> institutes, List<String> faculties,
      Map<String, Integer> personMap) {
    setMargin(true);

    this.personMap = personMap;

    groupName = new TextField("Group Name");
    groupName.setWidth("300px");
    addComponent(groupName);

    acronym = new TextField("Acronym");
    acronym.setWidth("300px");
    addComponent(Styles.questionize(acronym,
        "Short acronym of the lowest level of this affiliation, "
            + "e.g. of the group if specified or of the institute if group field is left empty.",
        "Acronym"));

    organization = new TextField("Organization");
    organization.setWidth("300px");
    organization.setRequired(true);
    organization.setInputPrompt("...or university name");
    organization.setDescription("Organization or University Name");
    addComponent(organization);

    institute = new ComboBox("Institute", institutes);
    institute.setWidth("300px");
    institute.setNewItemsAllowed(true);
    institute.setStyleName(ValoTheme.COMBOBOX_SMALL);
    institute.setFilteringMode(FilteringMode.CONTAINS);
    // institute.setRequired(true);
    addComponent(Styles.questionize(institute, "Select existing institutes or input a new one.",
        "Institute"));

    faculty = new ComboBox("Faculty", faculties);
    faculty.setRequired(true);
    faculty.setStyleName(ValoTheme.COMBOBOX_SMALL);
    faculty.setWidth("300px");
    addComponent(Styles.questionize(faculty,
        "Faculty of the institute/affiliation. University affiliations like QBiC "
            + "that are neither part of Medical nor Science Faculty belong to Central Units. "
            + "For non-university affiliations select Other.",
        "Faculty"));

    contactPerson = new ComboBox("Contact Person", personMap.keySet());
    contactPerson.setWidth("300px");
    contactPerson.setFilteringMode(FilteringMode.CONTAINS);
    contactPerson.setStyleName(ValoTheme.COMBOBOX_SMALL);
    // contactPerson.setRequired(true);
    addComponent(Styles.questionize(contactPerson, "Main contact person of this affiliation.",
        "Contact Person"));

    head = new ComboBox("Head", personMap.keySet());
    head.setWidth("300px");
    head.setFilteringMode(FilteringMode.CONTAINS);
    // head.setRequired(true);
    head.setStyleName(ValoTheme.COMBOBOX_SMALL);
    addComponent(Styles.questionize(head, "Head of this affiliation.", "Head"));

    street = new TextField("Street");
    street.setWidth("300px");
    street.setRequired(true);
    addComponent(street);

    zipCode = new TextField("Zip Code");
    zipCode.setWidth("300px");
    zipCode.setRequired(true);
    addComponent(zipCode);

    city = new TextField("City");
    city.setWidth("300px");
    city.setRequired(true);
    addComponent(city);

    country = new TextField("Country");
    country.setWidth("300px");
    country.setRequired(true);
    addComponent(country);

    webpage = new TextField("Webpage");
    webpage.setWidth("300px");
    // TODO webpage formats are difficult
    // webpage.addValidator(
    // new RegexpValidator(Helpers.VALID_URL_REGEX, "This is not a valid web page format."));
    addComponent(webpage);

    commit = new Button("Register Affiliation");
    addComponent(commit);
  }

  public boolean isValid() {
    return groupName.isValid() && acronym.isValid() && organization.isValid() && institute.isValid()
        && faculty.isValid() && contactPerson.isValid() && head.isValid() && street.isValid()
        && zipCode.isValid() && country.isValid() && city.isValid() && webpage.isValid();
  }

  public Button getCommitButton() {
    return commit;
  }

  private int mapPersonToID(String person) {
    if (person == null) {
      logger.info("No optional person provided for new affiliation. Field will be empty.");
      return -1;
    } else
      return personMap.get(person);
  }

  public TextField getOrganizationField() {
    return organization;
  }

  public Affiliation getAffiliation() {
    String inst = null;
    if (institute.getValue() != null)
      inst = institute.getValue().toString();
    String contact = null;
    if (contactPerson.getValue() != null)
      contact = contactPerson.getValue().toString();
    String headPerson = null;
    if (head.getValue() != null)
      headPerson = head.getValue().toString();

    int contactID = mapPersonToID(contact);
    int headID = mapPersonToID(headPerson);

    String fac = faculty.getValue().toString();
    return new Affiliation(groupName.getValue(), acronym.getValue(), organization.getValue(), inst,
        fac, contactID, headID, street.getValue(), zipCode.getValue(), city.getValue(),
        country.getValue(), webpage.getValue());
  }

  public void autoComplete(Affiliation affiliation) {
    organization.setValue(affiliation.getOrganization());
    institute.setValue(affiliation.getInstitute());
    faculty.setValue(affiliation.getFaculty());
    street.setValue(affiliation.getStreet());
    city.setValue(affiliation.getCity());
    zipCode.setValue(affiliation.getZipCode());
    country.setValue(affiliation.getCountry());
  }

  public ComboBox getInstituteField() {
    return institute;
  }
  
  public void hideRegisterButton() {
    commit.setEnabled(false);
    commit.setVisible(false);
  }

  public void reset() {
    groupName.setValue("");
    acronym.setValue("");
    organization.setValue("");
    institute.setValue(institute.getNullSelectionItemId());
    faculty.setValue(faculty.getNullSelectionItemId());
    contactPerson.setValue(contactPerson.getNullSelectionItemId());
    head.setValue(head.getNullSelectionItemId());
    street.setValue("");
    zipCode.setValue("");
    city.setValue("");
    country.setValue("");
    webpage.setValue("");
  }

}
