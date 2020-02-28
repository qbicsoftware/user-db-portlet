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
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.CompositeValidator;
import com.vaadin.data.validator.CompositeValidator.CombinationMode;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;
import life.qbic.datamodel.persons.Affiliation;
import life.qbic.datamodel.persons.Person;
import life.qbic.portal.Styles;
import life.qbic.userdb.helpers.RegExHelper;

public class PersonInput extends HorizontalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = 2657654653139639151L;
  private FormLayout left;

  private Button commit;
  private TextField userName;
  private ComboBox title;
  private TextField first;
  private TextField last;
  private TextField eMail;
  private TextField phone;
  private ComboBox affiliation;
  private CheckBox newAffiliation;
  private AffiliationInput affiInput;
  private ComboBox role;

  private Map<String, Integer> affiliationMap;

  public PersonInput(List<String> titles, Map<String, Integer> affiliations, List<String> roles,
      Map<String, Integer> colNamesToMaxLength, AffiliationInput affiInput) {
    left = new FormLayout();
    left.setMargin(true);

    affiliationMap = affiliations;
    this.affiInput = affiInput;
    this.affiInput.hideRegisterButton();
    this.affiInput.setVisible(false);

    userName = new TextField("Username");
    // userName.setRequired(true);
    userName.addValidator(
        new RegexpValidator(RegExHelper.VALID_USERNAME_REGEX, "Please input a valid username."));
    left.addComponent(Styles.questionize(userName,
        "University Tübingen user name or user name provided by QBiC. If left empty a dummy user name is chosen "
            + "which cannot be used to log in until a real name is added. Person information can still be added to "
            + "projects or experiments in that case.",
        "User Name"));

    title = new ComboBox("Title", titles);
    title.setRequired(true);
    title.setStyleName(ValoTheme.COMBOBOX_SMALL);
    title.setNullSelectionAllowed(false);
    left.addComponent(title);

    Validator nameValidator =
        new RegexpValidator(RegExHelper.VALID_NAME_REGEX, "Please input a valid name.");
    first = prepSizeValidationForTextField("First Name", colNamesToMaxLength.get("first_name"),
        nameValidator);
    first.setRequired(true);
    left.addComponent(first);

    Validator nameValidator2 =
        new RegexpValidator(RegExHelper.VALID_NAME_REGEX, "Please input a valid name.");
    last = prepSizeValidationForTextField("Last Name", colNamesToMaxLength.get("family_name"),
        nameValidator2);
    last.setRequired(true);
    left.addComponent(last);

    Validator mailValidator = new RegexpValidator(RegExHelper.VALID_EMAIL_ADDRESS_REGEX,
        "Please input a valid e-mail address.");
    eMail =
        prepSizeValidationForTextField("E-Mail", colNamesToMaxLength.get("email"), mailValidator);
    eMail.setRequired(true);
    left.addComponent(eMail);

    phone = prepSizeValidationForTextField("Phone", colNamesToMaxLength.get("phone"), null);
    left.addComponent(phone);

    affiliation = new ComboBox("Affiliation", affiliations.keySet());
    // affiliation.setNullSelectionAllowed(false);
    affiliation.setRequired(true);
    affiliation.setFilteringMode(FilteringMode.CONTAINS);
    affiliation.setStyleName(ValoTheme.COMBOBOX_SMALL);
    left.addComponent(Styles.questionize(affiliation,
        "Work group or organization this person is part of. If it does not exist in the system "
            + "a \"New Affiliation\" has to be created first. Additional Affiliations and roles can be set in the next Tab.",
        "Affiliation"));

    newAffiliation = new CheckBox("New Affiliation");
    left.addComponent(newAffiliation);
    newAffiliation.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        enableAffiliationInput(newAffiliation.getValue());
        affiliation.select(affiliation.getNullSelectionItemId());
        affiliation.setEnabled(!newAffiliation.getValue());
      }
    });

    role = new ComboBox("Role", roles);
    role.setRequired(true);
    role.setStyleName(ValoTheme.COMBOBOX_SMALL);
    role.setNullSelectionAllowed(false);
    left.addComponent(role);

    commit = new Button("Save New User");
    left.addComponent(commit);

    addComponent(left);
    addComponent(affiInput);
  }

  protected void enableAffiliationInput(Boolean enable) {
    if (enable)
      affiInput.reset();
    affiInput.setVisible(enable);
  }

  public boolean isValid() {
    boolean affiliationValid = affiInput.isValid() || affiliation.isValid();
    return userName.isValid() && title.isValid() && first.isValid() && last.isValid()
        && title.isValid() && eMail.isValid() && phone.isValid() && affiliationValid
        && role.isValid();
  }

  public Button getCommitButton() {
    return commit;
  }

  public Person getPerson() {
    String ttl = null;
    if (title.getValue() != null)
      ttl = title.getValue().toString();
    String affRole = null;
    if (role.getValue() != null)
      affRole = role.getValue().toString();
    String affi = (String) affiliation.getValue();
    int affiID = -1;
    if (affiliationMap.containsKey(affi))
      affiID = affiliationMap.get(affi);
    return new Person(userName.getValue(), ttl, first.getValue(), last.getValue(), eMail.getValue(),
        phone.getValue(), affiID, affi, affRole);
  }

  public boolean hasNewAffiliation() {
    return newAffiliation.getValue();
  }

  public Affiliation getNewAffiliation() {
    return affiInput.getAffiliation();
  }

  private CompositeValidator prepCompositeValidator(Validator v) {
    CompositeValidator res = new CompositeValidator(CombinationMode.AND, "");
    res.addValidator(v);
    return res;
  }

  private TextField prepSizeValidationForTextField(String name, int maxLength, Validator val) {
    TextField t = new TextField(name);
    StringLengthValidator lengthVal =
        new StringLengthValidator(name + " needs to contain less than " + maxLength + " symbols.");
    lengthVal.setMaxLength(maxLength);
    if (val != null) {
      CompositeValidator composite = prepCompositeValidator(val);
      composite.addValidator(lengthVal);
      t.addValidator(composite);
    } else {
      t.addValidator(lengthVal);
    }
    return t;
  }
}
