package life.qbic.userdb.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import life.qbic.userdb.model.Person;
import life.qbic.userdb.model.Person.PersonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PersonBatchReader {

  private static final Logger logger = LogManager.getLogger(PersonBatchReader.class);
  private List<String> mandatory;
  private List<String> optional;

  private String error;
  private List<String> tsvByRows;
  private List<Person> people;

  public PersonBatchReader() {
    Set<String> mandatory =
        new HashSet<String>(Arrays.asList("first name", "last name", "title", "email"));
    this.mandatory = new ArrayList<String>(mandatory);
    Set<String> optional = new HashSet<String>(Arrays.asList("phone", "username"));
    this.optional = new ArrayList<String>(optional);
  }

  public static void main(String[] args) throws JAXBException {
    try {
      PersonBatchReader p = new PersonBatchReader();
      if (p.readPeopleFile(new File("person_import.tsv")))
        System.out.println(p.getPeople());
      else
        System.out.println(p.getError());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public List<Person> getPeople() {
    return people;
  }

  public static final String UTF8_BOM = "\uFEFF";

  private static String removeUTF8BOM(String s) {
    if (s.startsWith(UTF8_BOM)) {
      s = s.substring(1);
    }
    return s;
  }

  /**
   * Reads in a TSV file containing samples that should be registered. Returns a List of
   * TSVSampleBeans containing all the necessary information to register each sample with its meta
   * information to openBIS, given that the types and parents exist.
   * 
   * @param file
   * @return ArrayList of TSVSampleBeans
   * @throws IOException
   * @throws JAXBException
   */
  public boolean readPeopleFile(File file) throws IOException {
    logger.info("parsing uploaded file: "+file.getAbsolutePath());
    people = new ArrayList<Person>();
    tsvByRows = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    ArrayList<String[]> data = new ArrayList<String[]>();
    String next;
    int i = 0;
    // isPilot = false;
    while ((next = reader.readLine()) != null) {
      i++;
      next = removeUTF8BOM(next);
      tsvByRows.add(next);
      String[] nextLine = next.split("\t", -1);// this is needed for trailing tabs
      if (data.isEmpty() || nextLine.length == data.get(0).length) {
        data.add(nextLine);
      } else {
        error = "Wrong number of columns in row " + i;
        reader.close();
        return false;
      }
    }
    reader.close();

    if(data.isEmpty()) {
      error = "File is empty.";
      return false;
    }
    String[] header = data.get(0);
    data.remove(0);

    Map<String, Integer> headerMapping = new HashMap<String, Integer>();

    ArrayList<String> found = new ArrayList<String>(Arrays.asList(header));
    for (int j = 0; j < found.size(); j++)
      found.set(j, found.get(j).toLowerCase());

    for (String col : mandatory) {
      if (!found.contains(col)) {
        error = "Mandatory column " + col + " not found.";
        return false;
      }
    }
    for (i = 0; i < header.length; i++) {
      String name = header[i].toLowerCase();
      int position = mandatory.indexOf(name);
      if (position > -1) {
        headerMapping.put(name, i);
      } else {
        position = optional.indexOf(name);
        if (position > -1) {
          headerMapping.put(name, i);
        }
      }

    }
    // create person objects
    int rowID = 0;
    for (String[] row : data) {
      rowID++;
      for (String col : mandatory) {
        if (row[headerMapping.get(col)].isEmpty()) {
          error = col + " is a mandatory field, but it is not set for row " + rowID + "!";
          return false;
        }
      }

      String first = row[headerMapping.get("first name")];
      String last = row[headerMapping.get("last name")];
      String title = row[headerMapping.get("title")];
      String mail = row[headerMapping.get("email")];
      PersonBuilder personBuilder = new PersonBuilder();
      personBuilder.createPerson(title, first, last, mail);
      if (headerMapping.get("phone") != null)
        personBuilder.withPhoneNumber(row[headerMapping.get("phone")]);
      if (headerMapping.get("username") != null)
        personBuilder.withUsername(row[headerMapping.get("username")]);
      people.add(personBuilder.getPerson());
    }
    return true;
  }

  public String getError() {
    if (error != null)
      logger.error(error);
    else
      logger.info("Parsing of experimental design successful.");
    return error;
  }

  public List<String> getTSVByRows() {
    return tsvByRows;
  }

}
