package com.playground.fondoahorro.domain.outputport;

import java.util.Optional;

/** Generic key/value access to app_settings (loan interest, settlement interest, birthday gift default, ...). */
public interface AppSettingsRepository {

    Optional<String> get(String key);

    void set(String key, String value);
}
