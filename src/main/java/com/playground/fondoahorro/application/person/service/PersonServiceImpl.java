package com.playground.fondoahorro.application.person.service;

import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.domain.inputport.PersonService;
import com.playground.fondoahorro.domain.outputport.PersonRepository;
import com.playground.fondoahorro.domain.vo.PersonSummary;
import com.playground.fondoahorro.infrastructure.transaction.TransactionRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PersonServiceImpl implements PersonService {

    private final PersonRepository repository;

    public PersonServiceImpl(PersonRepository repository) {
        this.repository = repository;
    }

    @Override
    public Person createPerson(String name, LocalDate birthDate, String phone) {
        return TransactionRunner.run(connection -> createPerson(connection, name, birthDate, phone));
    }

    /** Same as createPerson(...), but runs on a caller-supplied connection — used by the Excel importer to create every person inside one shared transaction. */
    public Person createPerson(Connection connection, String name, LocalDate birthDate, String phone) throws SQLException {
        Person person = Person.newPerson(name, birthDate, phone);
        return repository.insert(connection, person);
    }

    public Person updatePerson(long id, String name, LocalDate birthDate, String phone) {
        Person existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La persona no existe."));
        Person updated = existing.withUpdatedDetails(name, birthDate, phone);
        repository.update(updated);
        return repository.findById(id).orElseThrow();
    }

    public void setActive(long id, boolean active) {
        repository.findById(id).orElseThrow(() -> new IllegalArgumentException("La persona no existe."));
        repository.setActive(id, active);
    }

    public Optional<Person> findById(long id) {
        return repository.findById(id);
    }

    public List<Person> list(String query, boolean includeInactive) {
        return repository.findAll(query, includeInactive);
    }

    public List<PersonSummary> listWithSummary(String query, boolean includeInactive) {
        return repository.findAllWithSummary(query, includeInactive);
    }

    public PersonSummary getSummary(long id) {
        return repository.getSummary(id);
    }
}
