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
package life.qbic.userdb;

import java.util.List;
import java.util.Map;


public class DBVocabularies {

  private Map<String, String> taxMap;
  private Map<String, String> tissueMap;
  private Map<String, String> deviceMap;
  private Map<String, String> cellLinesMap;
  private List<String> measureTypes;
  private List<String> spaces;
  private Map<String, Integer> investigators;
  private List<String> experimentTypes;
  private List<String> enzymes;
  private List<String> msProtocols;
  private List<String> lcmsMethods;
  private List<String> chromTypes;

  public DBVocabularies(Map<String, String> taxMap, Map<String, String> tissueMap,
      Map<String, String> cellLinesMap, List<String> measureTypes, List<String> spaces,
      Map<String, Integer> piMap, List<String> experimentTypes, List<String> enzymes,
      Map<String, String> deviceMap, List<String> msProtocols, List<String> lcmsMethods,
      List<String> chromTypes) {
    this.taxMap = taxMap;
    this.tissueMap = tissueMap;
    this.cellLinesMap = cellLinesMap;
    this.deviceMap = deviceMap;
    this.measureTypes = measureTypes;
    this.spaces = spaces;
    this.investigators = piMap;
    this.experimentTypes = experimentTypes;
    this.enzymes = enzymes;
    this.msProtocols = msProtocols;
    this.lcmsMethods = lcmsMethods;
    this.chromTypes = chromTypes;
  }

  public Map<String, String> getCellLinesMap() {
    return cellLinesMap;
  }

  public Map<String, String> getTaxMap() {
    return taxMap;
  }

  public Map<String, String> getTissueMap() {
    return tissueMap;
  }

  public Map<String, String> getDeviceMap() {
    return deviceMap;
  }

  public List<String> getMeasureTypes() {
    return measureTypes;
  }

  public List<String> getSpaces() {
    return spaces;
  }

  public Map<String, Integer> getInvestigators() {
    return investigators;
  }

  public List<String> getExperimentTypes() {
    return experimentTypes;
  }

  public List<String> getEnzymes() {
    return enzymes;
  }

  public List<String> getMsProtocols() {
    return msProtocols;
  }

  public List<String> getLcmsMethods() {
    return lcmsMethods;
  }

  public List<String> getChromTypes() {
    return chromTypes;
  }

}
