Feature: Sort the suppliers on screen
  As a buyer
  I want to reorder the results by any column
  So that I can compare suppliers by the criterion that matters to me

  Background:
    Given the reference supplier catalogue is available
    And the user is on the potential suppliers dashboard
    And the user has searched for potential suppliers for an order of 5000000 €

  Scenario: Sorting by a column, then reversing it
    When the user sorts the results by "Name"
    Then the results are sorted by "Name" in ascending order
    And the results show these suppliers in this order:
      | Name              |
      | Berlin Fasteners  |
      | Cantabria Cotton  |
      | Galicia Knits     |
      | Hamburg Threads   |
      | Iberian Textiles  |
      | Lyon Silks        |
      | Marseille Leather |
      | Milano Moda       |
      | Torino Wool       |
      | Zippers & Buttons |
    When the user sorts the results by "Name"
    Then the results are sorted by "Name" in descending order
    And the first supplier shown is "Zippers & Buttons"
    And the last supplier shown is "Berlin Fasteners"

  Scenario: Sorting by annual turnover compares amounts, smallest first
    When the user sorts the results by "Annual Turnover"
    Then the results are sorted by "Annual Turnover" in ascending order
    And the results are no longer sorted by "Score"
    And the first supplier shown is "Zippers & Buttons"
    And the last supplier shown is "Torino Wool"
