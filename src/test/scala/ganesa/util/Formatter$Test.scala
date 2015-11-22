package ganesa.util

import test.UnitSpec

class Formatter$Test extends UnitSpec {

  before {
  }

  after {
  }

  override def beforeAll(): Unit = {
  }

  override def afterAll(): Unit = {
  }

  describe("formatNumber") {
    it("should format numbers with comma correctly") {
      assert(Formatter.formatNumber(0) == "0")
      assert(Formatter.formatNumber(10) == "10")
      assert(Formatter.formatNumber(100) == "100")
      assert(Formatter.formatNumber(1000) == "1,000")
      assert(Formatter.formatNumber(10000) == "10,000")
      assert(Formatter.formatNumber(100000) == "100,000")
      assert(Formatter.formatNumber(1000000) == "1,000,000")
    }
  }
}
