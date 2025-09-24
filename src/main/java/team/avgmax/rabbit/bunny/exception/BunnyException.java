package team.avgmax.rabbit.bunny.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BunnyException extends RuntimeException {

    private final BunnyError error;

    @Override
    public String getMessage() {
        return error.getMessage();
    }
}
