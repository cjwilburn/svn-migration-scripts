import java.io.ByteArrayInputStream
import org.scalatest.FunSuite

class AuthorsTest extends FunSuite {

  def parseUserXml(xml: String, expected: String*) = {
    val authors = collection.mutable.Set[String]()
    Authors.parseUserXml(authors)(new ByteArrayInputStream(xml.getBytes));
    assert(authors === expected.toSet)
  }

  test("parseUserXmlSingle") {
    parseUserXml("""<log><logentry><author>a</author></logentry></log>""", "a")
  }

  test("parseUserXmlMulti") {
    parseUserXml("""<log><logentry><author>a</author><author>b</author></logentry></log>""", "a", "b")
  }
}
