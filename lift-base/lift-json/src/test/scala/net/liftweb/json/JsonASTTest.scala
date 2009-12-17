package net.liftweb.json

/* FIXME 280
import _root_.org.scalacheck._
import _root_.org.scalacheck.Prop.forAll
import _root_.org.specs.Specification
import _root_.org.specs.runner.{Runner, JUnit}
import _root_.org.specs.ScalaCheck

class JsonASTTest extends Runner(JsonASTSpec) with JUnit
object JsonASTSpec extends Specification with JValueGen with ScalaCheck {
  import JsonAST._

  "Functor identity" in {
    val identityProp = (json: JValue) => json == (json map identity)
    forAll(identityProp) must pass
  }

  "Functor composition" in {
    val compositionProp = (json: JValue, fa: JValue => JValue, fb: JValue => JValue) => 
      json.map(fb).map(fa) == json.map(fa compose fb) 

    forAll(compositionProp) must pass
  }

  "Monoid identity" in {
    val identityProp = (json: JValue) => (json ++ JNothing == json) && (JNothing ++ json == json)
    forAll(identityProp) must pass    
  }

  "Monoid associativity" in {
    val assocProp = (x: JValue, y: JValue, z: JValue) => x ++ (y ++ z) == (x ++ y) ++ z
    forAll(assocProp) must pass
  }

  "Merge identity" in {
    val identityProp = (json: JValue) => (json merge JNothing) == json && (JNothing merge json) == json
    forAll(identityProp) must pass
  }

  "Merge idempotency" in {
    val idempotencyProp = (x: JValue) => (x merge x) == x
    forAll(idempotencyProp) must pass
  }

  "Diff identity" in {
    val identityProp = (json: JValue) => 
      (json diff JNothing) == Diff(JNothing, JNothing, json) && 
      (JNothing diff json) == Diff(JNothing, json, JNothing)

    forAll(identityProp) must pass
  }

  "Diff with self is empty" in {
    val emptyProp = (x: JValue) => (x diff x) == Diff(JNothing, JNothing, JNothing)
    forAll(emptyProp) must pass
  }

  "Diff is subset of originals" in {
    val subsetProp = (x: JObject, y: JObject) => {
      val Diff(c, a, d) = x diff y
      y == (y merge (c merge a))
    }
    forAll(subsetProp) must pass
  }

  "Diff result is same when fields are reordered" in {
    val reorderProp = (x: JObject) => (x diff reorderFields(x)) == Diff(JNothing, JNothing, JNothing)
    forAll(reorderProp) must pass
  } 

  "Remove all" in {
    val removeAllProp = (x: JValue) => (x remove { _ => true }) == JNothing
    forAll(removeAllProp) must pass
  }

  "Remove nothing" in {
    val removeNothingProp = (x: JValue) => (x remove { _ => false }) == x
    forAll(removeNothingProp) must pass
  }

  "Remove removes only matching elements (in case of a field, its value is set to JNothing)" in {
    val removeProp = (json: JValue, x: Class[_ <: JValue]) => {
      val removed = json remove typePredicate(x)
      val Diff(c, a, d) = json diff removed
      val elemsLeft = removed filter {
        case JField(_, JNothing) => false
        case _ => true 
      }
      c == JNothing && a == JNothing && elemsLeft.forall(_.getClass != x)
    }
    forAll(removeProp) must pass
  }

  private def reorderFields(json: JValue) = json map {
    case JObject(xs) => JObject(xs.reverse)
    case x => x
  }

  private def typePredicate(clazz: Class[_])(json: JValue) = json match {
    case x if x.getClass == clazz => true
    case _ => false
  }

  implicit def arbJValue: Arbitrary[JValue] = Arbitrary(genJValue)
  implicit def arbJObject: Arbitrary[JObject] = Arbitrary(genObject)
  implicit def arbJValueClass: Arbitrary[Class[_ <: JValue]] = Arbitrary(genJValueClass)
}

*/