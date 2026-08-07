package com.playground.fondoahorro.application.movement;

import com.playground.fondoahorro.domain.movement.MovementKind;
import com.playground.fondoahorro.domain.movement.MovementType;
import com.playground.fondoahorro.domain.movement.MovementTypeRepository;

import java.util.List;
import java.util.Optional;

public class MovementTypeService {

    private final MovementTypeRepository repository;

    public MovementTypeService(MovementTypeRepository repository) {
        this.repository = repository;
    }

    public MovementType createCustomType(String name, MovementKind kind) {
        return repository.insert(MovementType.newCustomType(name, kind));
    }

    public void rename(long id, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Ingresa el nombre del tipo.");
        }
        repository.findById(id).orElseThrow(() -> new IllegalArgumentException("El tipo no existe."));
        repository.rename(id, newName.trim());
    }

    public void setActive(long id, boolean active) {
        MovementType type = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El tipo no existe."));
        if (type.isSystemType() && !active) {
            throw new IllegalArgumentException(
                    "\"" + type.name() + "\" es un tipo del sistema y no se puede desactivar.");
        }
        repository.setActive(id, active);
    }

    public Optional<MovementType> findByCode(String code) {
        return repository.findByCode(code);
    }

    public List<MovementType> listAll(boolean includeInactive) {
        return repository.findAll(includeInactive);
    }

    public List<MovementType> listByKind(MovementKind kind, boolean includeInactive) {
        return repository.findByKind(kind, includeInactive);
    }
}
