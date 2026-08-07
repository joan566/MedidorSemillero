package com.playground.fondoahorro.domain.person;

import java.util.List;
import java.util.Optional;

public interface PersonRepository {

    Person insert(Person person);

    void update(Person person);

    void setActive(long id, boolean active);

    Optional<Person> findById(long id);

    List<Person> findAll(String query, boolean includeInactive);

    List<PersonSummary> findAllWithSummary(String query, boolean includeInactive);

    PersonSummary getSummary(long id);
}
