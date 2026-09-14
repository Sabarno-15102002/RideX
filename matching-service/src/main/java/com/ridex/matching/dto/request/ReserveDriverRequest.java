package com.ridex.matching.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ReserveDriverRequest(
        @NotNull UUID tripId
) {}