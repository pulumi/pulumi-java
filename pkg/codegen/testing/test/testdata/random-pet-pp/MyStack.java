package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static Exports stack(Context ctx) {
        var random_pet = new RandomPet("random_pet", RandomPetArgs.builder()        
            .prefix("doggo")
            .build());

        return ctx.exports();
    }
}
