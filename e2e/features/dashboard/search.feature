Feature: Search potential suppliers for an order
  As a buyer preparing an order
  I want to see which suppliers can take an order of a given amount
  So that I can pick the best-scored one

  The reference catalogue holds 12 approved suppliers in ES, FR, DE and IT with turnovers
  between 10.000.000 € and 90.000.000 € (see e2e/support/reference-suppliers.ts). Expected
  scores include the 25% small-supplier bonus of the two lowest turnovers of each country,
  which does not depend on the order amount (e.g. Hamburg Threads keeps it at 50.000.000 €).

  Background:
    Given the reference supplier catalogue is available
    And the user is on the potential suppliers dashboard

  Scenario: The dashboard is ready for a search when it first loads
    Then the user is asked for an order amount
    And no supplier results are shown

  Scenario: Eligible suppliers are listed by score, best first
    When the user searches for potential suppliers for an order of 5000000 €
    Then 12 suppliers are reported as found
    And the results are sorted by "Score" in descending order
    And the results show these suppliers in this order:
      | DUNS      | Name              | Country | Annual Turnover | Rating | Score        |
      | 900000004 | Galicia Knits     | ES      | 80.000.000 €    | A      | 8.000.000,00 |
      | 900000010 | Hamburg Threads   | DE      | 60.000.000 €    | A      | 7.500.000,00 |
      | 900000007 | Marseille Leather | FR      | 50.000.000 €    | B      | 3.750.000,00 |
      | 900000011 | Milano Moda       | IT      | 35.000.000 €    | B      | 3.281.250,00 |
      | 900000012 | Torino Wool       | IT      | 90.000.000 €    | D      | 2.812.500,00 |
      | 900000003 | Cantabria Cotton  | ES      | 40.000.000 €    | C      | 2.000.000,00 |
      | 900000002 | Iberian Textiles  | ES      | 20.000.000 €    | B      | 1.875.000,00 |
      | 900000009 | Berlin Fasteners  | DE      | 25.000.000 €    | C      | 1.562.500,00 |
      | 900000005 | Lyon Silks        | FR      | 12.000.000 €    | A      | 1.500.000,00 |
      | 900000001 | Zippers & Buttons | ES      | 10.000.000 €    | A      | 1.250.000,00 |

  Scenario: Only suppliers whose turnover exceeds the order amount are eligible
    When the user searches for potential suppliers for an order of 50000000 €
    Then 4 suppliers are reported as found
    And the results show these suppliers in this order:
      | Name            | Annual Turnover | Score        |
      | Galicia Knits   | 80.000.000 €    | 8.000.000,00 |
      | Hamburg Threads | 60.000.000 €    | 7.500.000,00 |
      | Torino Wool     | 90.000.000 €    | 2.812.500,00 |
      | Normandy Linen  | 70.000.000 €    | 700.000,00   |

  Scenario: The minimum order amount of 250 € is accepted
    When the user searches for potential suppliers for an order of 250 €
    Then 12 suppliers are reported as found

  Scenario: No supplier can take an order larger than every turnover
    When the user searches for potential suppliers for an order of 90000000 €
    Then the user is told that no potential suppliers were found for this amount

  Scenario Outline: An order amount of <amount> € is rejected before searching
    When the user searches for potential suppliers for an order of <amount> €
    Then the user is told "Amount must be at least 250"
    And no search is sent to the server
    And no supplier results are shown

    Examples:
      | amount |
      | 249    |
      | 0      |
      | -500   |

  Scenario: Searching without an order amount is rejected
    When the user searches without entering an order amount
    Then the user is told "Amount must be at least 250"
    And no search is sent to the server
