package com.playground.fondoahorro.domain.inputport;

import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.domain.vo.PersonSummary;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PersonService {

    Person createPerson(String name, LocalDate birthDate, String phone);

    /** Same as createPerson(...), but runs on a caller-supplied connection — used by the Excel importer to create every person inside one shared transaction. */
    Person createPerson(Connection connection, String name, LocalDate birthDate, String phone) throws SQLException;

    Person updatePerson(long id, String name, LocalDate birthDate, String phone);

    void setActive(long id, boolean active);

    Optional<Person> findById(long id);

    List<Person> list(String query, boolean includeInactive);

    List<PersonSummary> listWithSummary(String query, boolean includeInactive);

    PersonSummary getSummary(long id);
}
