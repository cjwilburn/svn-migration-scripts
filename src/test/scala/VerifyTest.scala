package com.atlassian.svn2git

import org.specs2.{ScalaCheck, mutable}
import org.scalacheck._
import org.scalacheck.Prop.forAll
import com.atlassian.svn2git.Verify.VersionComparator

class VerifyTest extends mutable.Specification with ScalaCheck {

  def lt(actual: String, required: String) = VersionComparator.lt(actual, required) must beTrue

  "lt simple" >> lt("1.2.3", "1.2.4")
  "lt ascii" >> lt("1.2.5", "1.2.40")
  "lt one major" >> lt("1.2.3", "1.3")
  "lt same major, no minor" >> lt("1.2", "1.2.4")
  "lt major/minor" >> lt("1.1", "1.2.4")
  "lt major" >> lt("1.0", "2.1")

  val version = Gen.listOf1(Gen.choose(1, Integer.MAX_VALUE))

  def dot(l: List[Int]) = l.mkString(".")

  def lessThan(a: List[Int], b: List[Int]) = a.zip(b).map {
    case (x, y) => x compareTo y
  }.dropWhile(_ == 0).headOption.map(_ < 0).getOrElse(true)

  "test lessThan" >> {
    lessThan(List(1, 2, 3), List(1, 3, 2)) must beTrue
    lessThan(List(1, 3, 2), List(1, 2, 3)) must beFalse
    lessThan(List(1, 4), List(1, 3, 4)) must beFalse
    lessThan(List(1, 4), List(1, 4, 4)) must beTrue
  }

  "lt property check" >> {
    forAll(version, version) {
      (a: List[Int], b: List[Int]) =>
        val (x, y) = if (lessThan(a, b)) (a, b) else (b, a)
        lt(dot(x), dot(y))
    }
  }

}
