@lifecycle
Feature: Supplier lifecycle as seen from the dashboard
  As a buyer
  I want the dashboard to offer only suppliers the company has actually approved
  So that I never place an order with a candidate, a refused or a banned supplier

  Supervisors manage candidacies through the API only, so these scenarios drive the real
  backend's lifecycle and then check what the dashboard shows. Every scenario creates its own
  suppliers, each in a country used by no other scenario.

  Background:
    Given the user is on the potential suppliers dashboard

  Scenario Outline: An accepted candidate is offered as a potential supplier (rating <rating>)
    Given "<name>" from "<country>" has applied to become a supplier
    And the supervisor has accepted "<name>" with sustainability rating "<rating>"
    When the user looks for "<name>" among the potential suppliers
    Then "<name>" is offered as a potential supplier with rating "<rating>"
    And the supplier API reports "<name>" as "Active"

    Examples:
      | name           | country | rating |
      | Adriatic Trims | HR      | A      |
      | Danube Dyes    | AT      | D      |

  Scenario: A banned supplier is no longer offered
    Given "Baltic Buttons" from "LV" has applied to become a supplier
    And the supervisor has accepted "Baltic Buttons" with sustainability rating "E"
    And the supervisor has banned "Baltic Buttons"
    When the user looks for "Baltic Buttons" among the potential suppliers
    Then "Baltic Buttons" is not offered as a potential supplier
    And the supplier API reports "Baltic Buttons" as "Disqualified"

  Scenario: A candidate still awaiting a decision is not offered
    Given "Celtic Cords" from "IE" has applied to become a supplier
    When the user looks for "Celtic Cords" among the potential suppliers
    Then "Celtic Cords" is not offered as a potential supplier

  Scenario: A refused candidate is not offered
    Given "Bohemia Clasps" from "CZ" has applied to become a supplier
    And the supervisor has refused "Bohemia Clasps"
    When the user looks for "Bohemia Clasps" among the potential suppliers
    Then "Bohemia Clasps" is not offered as a potential supplier

  Scenario: A candidate from a non-approved country cannot become a supplier
    Given "Nordic Zips" from "NO" has applied to become a supplier
    When the supervisor tries to accept "Nordic Zips" with sustainability rating "A"
    Then the acceptance is rejected with "Candidate can not be accepted"
    When the user looks for "Nordic Zips" among the potential suppliers
    Then "Nordic Zips" is not offered as a potential supplier
