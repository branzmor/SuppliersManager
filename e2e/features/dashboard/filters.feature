Feature: Narrow down the suppliers on screen
  As a buyer
  I want to filter the suppliers I am looking at by name, DUNS, country and rating
  So that I can find a specific supplier or the ones that suit my order

  Filters work on the page of results currently loaded; the result count keeps reporting the
  server's total alongside what is visible.

  Background:
    Given the reference supplier catalogue is available
    And the user is on the potential suppliers dashboard
    And the user has searched for potential suppliers for an order of 5000000 €

  Scenario: Finding a supplier by part of its name, ignoring case
    When the user filters the results by "milano"
    Then the results show these suppliers in this order:
      | Name        |
      | Milano Moda |
    And 1 of the 12 suppliers found is visible

  Scenario: Finding a supplier by its DUNS
    When the user filters the results by "900000010"
    Then the results show these suppliers in this order:
      | DUNS      | Name            |
      | 900000010 | Hamburg Threads |

  Scenario: Filtering by several countries
    When the user filters the results by the countries "DE, IT"
    Then the results show these suppliers in this order:
      | Name             | Country |
      | Hamburg Threads  | DE      |
      | Milano Moda      | IT      |
      | Torino Wool      | IT      |
      | Berlin Fasteners | DE      |
    And 4 of the 12 suppliers found are visible

  Scenario: Filtering by sustainability rating
    When the user filters the results by the ratings "A, C"
    Then the results show these suppliers in this order:
      | Name              | Rating |
      | Galicia Knits     | A      |
      | Hamburg Threads   | A      |
      | Cantabria Cotton  | C      |
      | Berlin Fasteners  | C      |
      | Lyon Silks        | A      |
      | Zippers & Buttons | A      |

  Scenario: Filters that match nothing on the page
    When the user filters the results by the countries "IT"
    And the user filters the results by the ratings "A"
    Then the user is told that no suppliers on this page match the selected filters
    And 0 of the 12 suppliers found are visible

  Scenario: A new search starts without the previous filters
    Given the user has filtered the results by "milano"
    And the user has filtered the results by the ratings "B"
    When the user searches for potential suppliers for an order of 50000000 €
    Then no filters are active
    And 4 suppliers are reported as found
