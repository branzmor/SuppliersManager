@network
Feature: Feedback while searching and when a search fails
  As a buyer
  I want to know when a search is in progress or has failed
  So that I never mistake a slow or broken search for "no suppliers"

  These scenarios intercept the browser's call to the potential-suppliers API to reproduce
  timing and failure conditions the real backend can't produce on demand. Everything else
  in them still runs against the real stack.

  Background:
    Given the user is on the potential suppliers dashboard

  Scenario: A loading indicator is shown while the search is in progress
    Given the reference supplier catalogue is available
    And the server is slow to answer supplier searches
    When the user searches for potential suppliers for an order of 5000000 €
    Then the user sees that suppliers are loading
    And no supplier results are shown
    When the server answers the search
    Then the user no longer sees that suppliers are loading
    And 12 suppliers are reported as found

  Scenario: The server's explanation is shown when a search fails
    Given the supplier search fails with status 500 and the message "invalid operation service not ready"
    When the user searches for potential suppliers for an order of 5000000 €
    Then the user is told "invalid operation service not ready"
    And no supplier results are shown

  Scenario: A generic explanation is shown when the server gives none
    Given the supplier search fails with status 503 and no message
    When the user searches for potential suppliers for an order of 5000000 €
    Then the user is told "Request failed with status 503"
    And no supplier results are shown

  Scenario: The user is told when the server cannot be reached
    Given the server cannot be reached
    When the user searches for potential suppliers for an order of 5000000 €
    Then the user is told "Unable to reach the server. Please check your connection and try again."
    And no supplier results are shown

  Scenario: Searching again after the server comes back
    Given the reference supplier catalogue is available
    And the server cannot be reached
    And the user has searched for potential suppliers for an order of 5000000 €
    And the user is told "Unable to reach the server. Please check your connection and try again."
    When the server becomes reachable again
    And the user searches for potential suppliers for an order of 5000000 €
    Then the user is no longer shown an error
    And 12 suppliers are reported as found
