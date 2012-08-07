package com.atlassian.svn2git

import java.io.ByteArrayInputStream
import org.specs2.mutable

class AuthorsTest extends mutable.Specification {

  def parseUserXml(xml: String, expected: String*) = {
    val authors = Authors.parseUserXml(new ByteArrayInputStream(xml.getBytes))
    authors.toSet must equalTo (expected.toSet)
  }

  "parseUserXmlSingle" >> {
    parseUserXml("""<log><logentry><author>a</author></logentry></log>""", "a")
  }

  "parseUserXmlMulti" >> {
    parseUserXml("""<log><logentry><author>a</author><author>b</author></logentry></log>""", "a", "b")
  }

  "parseUserXmlMultiDupes" >> {
    parseUserXml("""<log><logentry><author>a</author><author>b</author><author>a</author></logentry></log>""", "a", "b")
  }

  "onDemandBaseUrl" >> {
    Authors.onDemandBaseUrl("https://chocs.jira-dev.com/svn/CMN/foo") must equalTo(Some("chocs.jira-dev.com"))
  }

  "not onDemandBaseUrl" >> {
    Authors.onDemandBaseUrl("https://chocs.abc.com/svn/CMN/foo") must equalTo(None)
  }
}
