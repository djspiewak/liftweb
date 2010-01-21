package net.liftweb.mapper

import org.specs._
import _root_.org.specs.runner.{JUnit3, ConsoleRunner}

/* FIXME 280
class ManyToManySpecsAsTest extends JUnit3(ManyToManySpecs)
object ManyToManySpecsRunner extends ConsoleRunner(ManyToManySpecs)

object ManyToManySpecs extends Specification {
  val provider = DBProviders.H2MemoryProvider
  private def ignoreLogger(f: => AnyRef): Unit = ()
  def setupDB {
    provider.setupDB
    Schemifier.destroyTables_!!(ignoreLogger _,  Person, Company, PersonCompany)
    Schemifier.schemify(true, ignoreLogger _, Person, Company, PersonCompany)
  }
  def createPerson = {
    val person = new Person
    person.save
    val companies = (1 to 10).toList map { i =>
      val c = new Company
      c.name ()= i.toString
      c.save
      c
    }
    person.companies ++= companies
    person.save
    // Break some joins
    companies(3).delete_! // delete "4"
    companies(6).delete_! // delete "7"
    person.companies.refresh  // reload joins so joinEntity.company.obj isn't cached
    person
  }
  
  "ManyToMany" should {
    "skip broken joins in children" in {
      setupDB
      val person = createPerson
      person.companies.joins.length must_== 10
      person.companies.all.length must_== 8
    }
    "handle missing joins in insertAll" in {
      setupDB
      val person = createPerson
      val c = new Company
      c.name ()= "new"
      c.save
      person.companies.insertAll(7, Seq(c))
      person.companies(7).name must_== "new"
    }
  }

}



class Person extends LongKeyedMapper[Person] with IdPK with ManyToMany {
  def getSingleton = Person  
  object companies extends MappedManyToMany(PersonCompany, PersonCompany.person, PersonCompany.company, Company)
}
object Person extends Person with LongKeyedMetaMapper[Person]

class Company extends LongKeyedMapper[Company] with IdPK {
  def getSingleton = Company
  object name extends MappedString(this, 10)
}
object Company extends Company with LongKeyedMetaMapper[Company]

class PersonCompany extends Mapper[PersonCompany] {
  def getSingleton = PersonCompany
  object person extends MappedLongForeignKey(this, Person)
  object company extends MappedLongForeignKey(this, Company)
}
object PersonCompany extends PersonCompany with MetaMapper[PersonCompany]
FIXME 280 */