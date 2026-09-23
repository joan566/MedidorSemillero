package com.playground.fondoahorro.domain.outputport;

import java.util.List;
import java.util.Optional;
import com.playground.fondoahorro.domain.entity.MovementType;
import com.playground.fondoahorro.domain.enums.MovementKind;

public interface MovementTypeRepository {

    MovementType insert(MovementType type);

    void rename(long id, String newName);

    void setActive(long id, boolean active);

    Optional<MovementType> findById(long id);

    Optional<MovementType> findByCode(String code);

    List<MovementType> findAll(boolean includeInactive);

    List<MovementType> findByKind(MovementKind kind, boolean includeInactive);
}
