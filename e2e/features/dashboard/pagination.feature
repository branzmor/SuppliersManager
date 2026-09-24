Feature: Browse all potential suppliers page by page
  As a buyer
  I want to page through long result lists
  So that I can see every eligible supplier, 10 at a time

  Background:
    Given the reference supplier catalogue is available
    And the user is on the potential suppliers dashboard
    And the user has searched for potential suppliers for an order of 5000000 €

  Scenario: The first page shows the 10 best-scored suppliers
    Then the user is on page 1 of 2
    And 10 suppliers are shown
    And the user cannot go to the previous page

  Scenario: Moving to the next page and back
    When the user goes to the next page
    Then the user is on page 2 of 2
    And the results show these suppliers in this order:
      | Name           | Score      |
      | Paris Denim    | 937.500,00 |
      | Normandy Linen | 700.000,00 |
    And 12 suppliers are reported as found
    And the user cannot go to the next page
    When the user goes to the previous page
    Then the user is on page 1 of 2
    And the first supplier shown is "Galicia Knits"
