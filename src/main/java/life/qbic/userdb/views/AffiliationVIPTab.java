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

import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.portal.Styles;

public class AffiliationVIPTab extends FormLayout {

  private ComboBox affiTabOrgs;
  private ComboBox head;
  private ComboBox contact;
  private Button commitAffiTabButton;

  private Map<String, Integer> affiliationMap;
  private Map<String, Integer> personMap;
  private Map<Integer, Pair<String, String>> personAffiliationsInTable;


  public AffiliationVIPTab(Map<String, Integer> persons, Map<String, Integer> affiliations,
      Map<Integer, Pair<String, String>> affiPeople) {

    this.affiliationMap = affiliations;
    this.personMap = persons;
    this.personAffiliationsInTable = affiPeople;

    affiTabOrgs = new ComboBox("Affiliation", affiliations.keySet());
    affiTabOrgs.setStyleName(ValoTheme.COMBOBOX_SMALL);
    affiTabOrgs.setFilteringMode(FilteringMode.CONTAINS);
    affiTabOrgs.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        Object val = affiTabOrgs.getValue();
        contact.setVisible(val != null);
        head.setVisible(val != null);
        if (val != null) {
          String affiName = val.toString();
          int id = affiliations.get(affiName);
          Pair names = personAffiliationsInTable.get(id);
          contact.setValue(names.getLeft());
          head.setValue(names.getRight());
        }
      }
    });

    head = new ComboBox("Head", persons.keySet());
    head.setStyleName(ValoTheme.COMBOBOX_SMALL);
    head.setFilteringMode(FilteringMode.CONTAINS);
    head.setVisible(false);
    contact = new ComboBox("Contact Person", persons.keySet());
    contact.setStyleName(ValoTheme.COMBOBOX_SMALL);
    contact.setFilteringMode(FilteringMode.CONTAINS);
    contact.setVisible(false);
    ValueChangeListener personListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        boolean hasData = head.getValue() != null || contact.getValue() != null;
        commitAffiTabButton.setEnabled(hasData);
      }
    };
    head.addValueChangeListener(personListener);
    contact.addValueChangeListener(personListener);

    commitAffiTabButton = new Button("Save Contact");
    commitAffiTabButton.setEnabled(false);

    addComponent(affiTabOrgs);
    addComponent(head);
    addComponent(contact);
    commitAffiTabButton.setIcon(FontAwesome.SAVE);
    addComponent(Styles.questionize(commitAffiTabButton,
        "Add or change records to the selected people. "
            + "Existing people can only be replaced by a new selection, empty selections are ignored.",
        "Save Changes"));
  }


  public int getNewHeadID() {
    Object val = head.getValue();
    if (val != null) {
      String headName = val.toString();
      return personMap.get(headName);
    } else
      return -1;
  }

  public int getNewContactID() {
    Object val = contact.getValue();
    if (val != null) {
      String contactName = val.toString();
      return personMap.get(contactName);
    } else
      return -1;
  }

  public int getSelectedAffiTabID() {
    Object val = affiTabOrgs.getValue();
    if (val != null) {
      String affiName = val.toString();
      return affiliationMap.get(affiName);
    } else
      return -1;
  }

  public void updateVIPs() {
    int affi = getSelectedAffiTabID();
    Object ctct = contact.getValue();
    Object hd = head.getValue();
    if (ctct == null)
      ctct = personAffiliationsInTable.get(affi).getLeft();
    if (hd == null)
      hd = personAffiliationsInTable.get(affi).getRight();
    personAffiliationsInTable.put(affi,
        new ImmutablePair<String, String>((String) ctct, (String) hd));
  }

  public Button getSetHeadAndContactButton() {
    return commitAffiTabButton;
  }
}
