package com.playground.fondoahorro.application.person;

import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.domain.person.PersonRepository;
import com.playground.fondoahorro.domain.person.PersonSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PersonService {

    private final PersonRepository repository;

    public PersonService(PersonRepository repository) {
        this.repository = repository;
    }

    public Person createPerson(String name, LocalDate birthDate, String phone) {
        Person person = Person.newPerson(name, birthDate, phone);
        return repository.insert(person);
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
