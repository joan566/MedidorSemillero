package com.playground.fondoahorro.domain.outputport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.domain.vo.PersonSummary;

public interface PersonRepository {

    Person insert(Person person);

    /** Same as insert(Person), but runs on a caller-supplied connection — for callers (e.g. the Excel importer) that need it inside a larger transaction. */
    Person insert(Connection connection, Person person) throws SQLException;

    void update(Person person);

    void setActive(long id, boolean active);

    Optional<Person> findById(long id);

    /** Same as findById(long), but on a caller-supplied connection — needed to see rows inserted earlier in the same uncommitted transaction. */
    Optional<Person> findById(Connection connection, long id) throws SQLException;

    List<Person> findAll(String query, boolean includeInactive);

    List<PersonSummary> findAllWithSummary(String query, boolean includeInactive);

    PersonSummary getSummary(long id);
}
