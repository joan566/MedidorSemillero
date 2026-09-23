package com.playground.fondoahorro.domain.inputport;

import com.playground.fondoahorro.domain.entity.MovementType;
import com.playground.fondoahorro.domain.enums.MovementKind;

import java.util.List;
import java.util.Optional;

public interface MovementTypeService {

    MovementType createCustomType(String name, MovementKind kind);

    void rename(long id, String newName);

    void setActive(long id, boolean active);

    Optional<MovementType> findByCode(String code);

    List<MovementType> listAll(boolean includeInactive);

    List<MovementType> listByKind(MovementKind kind, boolean includeInactive);
}
