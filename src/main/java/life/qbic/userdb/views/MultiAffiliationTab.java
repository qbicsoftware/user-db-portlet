/*******************************************************************************
 * QBiC User DB Tools enables users to add people and affiliations to our mysql user database.
 * Copyright (C) 2016  Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.userdb.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.datamodel.persons.Person;
import life.qbic.datamodel.persons.PersonAffiliationConnectionInfo;
import life.qbic.datamodel.persons.RoleAt;

public class MultiAffiliationTab extends VerticalLayout {

  // private TabSheet tabs;
  // private FormLayout personTab;

  private ComboBox organization;
  private ComboBox person;
  private Button addToTable;

  private Table table;

  // private Button setContactPerson;
  private Button commit;

  // private FormLayout affiTab;


  private Map<String, Integer> affiliationMap;
  private Map<String, Integer> personMap;
  private List<String> availableRoles;
  private Map<Integer, Person> personAffiliationsInTable;
  private String currentPerson;

  public MultiAffiliationTab(Map<String, Integer> persons, Map<String, Integer> affiliations,
      List<String> roles) {
    // tabs = new TabSheet();
    // personTab = new FormLayout();
    setMargin(true);
    setSpacing(true);
    this.affiliationMap = affiliations;
    this.personMap = persons;
    this.availableRoles = roles;

    person = new ComboBox("Person", persons.keySet());
    person.setStyleName(ValoTheme.COMBOBOX_SMALL);
    person.setFilteringMode(FilteringMode.CONTAINS);
    person.setNullSelectionAllowed(false);
    addComponent(person);
    organization = new ComboBox("Organization", affiliations.keySet());
    organization.setNullSelectionAllowed(false);
    organization.setStyleName(ValoTheme.COMBOBOX_SMALL);
    organization.setFilteringMode(FilteringMode.CONTAINS);
    addComponent(organization);

    addToTable = new Button("Add to Preview");
    addComponent(addToTable);
    addToTable.setEnabled(false);

    table = new Table();
    table.setWidthUndefined();
    // table.addContainerProperty("Title", String.class, null);
    // table.addContainerProperty("First Name", String.class, null);
    // table.addContainerProperty("Family Name", String.class, null);
    table.addContainerProperty("Affiliation", String.class, null);
    table.setColumnWidth("Affiliation", 250);
    table.addContainerProperty("Role", ComboBox.class, null);
    // table.addContainerProperty("Main Contact", CheckBox.class, null);
    table.addContainerProperty("Remove", Button.class, null);
    table.setImmediate(true);
    table.setVisible(false);
    addComponent(table);

    commit = new Button("Save Changes");
    addComponent(commit);
    // tabs.addTab(personTab, "Edit Person");

    // tabs.addTab(affiTab, "Edit Affiliation");
    // addComponent(tabs);
  }

  public ComboBox getPersonBox() {
    return person;
  }

  public ComboBox getOrganizationBox() {
    return organization;
  }

  public Button getCommitButton() {
    return commit;
  }

  public List<PersonAffiliationConnectionInfo> getChangedAndNewConnections() {
    List<PersonAffiliationConnectionInfo> res = new ArrayList<PersonAffiliationConnectionInfo>();
    for (Object affiliationID : table.getItemIds()) {
      ComboBox roleBox = (ComboBox) table.getItem(affiliationID).getItemProperty("Role").getValue();
      // String first = (String) table.getItem(id).getItemProperty("First Name").getValue();
      // String last = (String) table.getItem(id).getItemProperty("Family Name").getValue();
      // String name = first + " " + last;
      int personID = personMap.get(currentPerson);
      String role = "";
      if (roleBox.getValue() != null)
        role = (String) roleBox.getValue();
      res.add(new PersonAffiliationConnectionInfo(personID, (int) affiliationID, role));
    }
    return res;
  }

  public void reactToPersonSelection(List<Person> personsWithAffiliations) {
    table.removeAllItems();
    personAffiliationsInTable = new HashMap<Integer, Person>();
    Person p = personsWithAffiliations.get(0);
    String title = p.getTitle();
    String first = p.getFirstName();
    String last = p.getLastName();
    currentPerson = first + " " + last;
    table.setCaption("Affiliations of " + title + " " + first + " " + last);
    addDataToTable(personsWithAffiliations);

    table.setVisible(true);
  }

  public void addDataToTable(List<Person> personsWithAffiliations) {
    for (Person p : personsWithAffiliations) {
      // String title = p.getTitle();
      // String first = p.getFirst();
      // String last = p.getLast();
      Map<Integer, RoleAt> map = p.getAffiliationInfos();
      for (Integer i : p.getAffiliationInfos().keySet()) {
        personAffiliationsInTable.put(i, p);
        String affiliation = map.get(i).getAffiliation();
        String role = map.get(i).getRole();
        List<Object> row = new ArrayList<Object>();
        // row.add(title);
        // row.add(first);
        // row.add(last);
        row.add(affiliation);
        ComboBox roleInput = new ComboBox("", availableRoles);
        roleInput.setStyleName(ValoTheme.COMBOBOX_SMALL);
        roleInput.setValue(role);
        row.add(roleInput);
        Button delete = new Button("Remove");
        row.add(delete);
        delete.setData(i);
        delete.addClickListener(new Button.ClickListener() {
          /**
           * 
           */
          private static final long serialVersionUID = 5414693256990177472L;

          @Override
          public void buttonClick(ClickEvent event) {
            Button b = event.getButton();
            Integer iid = (Integer) b.getData();
            table.removeItem(iid);
            table.setPageLength(table.size());
            personAffiliationsInTable.remove(iid);
          }
        });
        table.addItem(row.toArray(), i);
      }
    }
    table.setPageLength(table.size());
  }

  public boolean isValid() {
    // TODO Auto-generated method stub
    return true;
  }

  public boolean newAffiliationPossible() {
    if (organization.getValue() == null)
      return false;
    int selectedAffi = affiliationMap.get(organization.getValue());
    Collection<Person> peopleAffisInTable = personAffiliationsInTable.values();
    boolean in = false;
    for (Person p : peopleAffisInTable)
      in |= p.getAffiliationInfos().containsKey(selectedAffi);
    return organization.getValue() != null && !in;
  }

  public Button getAddButton() {
    return addToTable;
  }
}
