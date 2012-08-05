import java.io.ByteArrayInputStream
import org.specs2.mutable

class AuthorsTest extends mutable.Specification {

  def parseUserXml(xml: String, expected: String*) = {
    val authors = collection.mutable.Set[String]()
    Authors.parseUserXml(authors)(new ByteArrayInputStream(xml.getBytes));
    authors must equalTo (expected.toSet)
  }

  "parseUserXmlSingle" >> {
    parseUserXml("""<log><logentry><author>a</author></logentry></log>""", "a")
  }

  "parseUserXmlMulti" >> {
    parseUserXml("""<log><logentry><author>a</author><author>b</author></logentry></log>""", "a", "b")
  }
}
