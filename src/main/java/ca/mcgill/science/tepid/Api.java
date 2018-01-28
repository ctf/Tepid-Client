package ca.mcgill.science.tepid;

import ca.mcgill.science.tepid.api.ITepid;
import ca.mcgill.science.tepid.api.TepidApi;
import ca.mcgill.science.tepid.client.Main;
import kotlin.Unit;
import retrofit2.Call;

import java.io.IOException;
import java.util.function.Function;

public class Api {

    private static ITepid instance = null;

    public static ITepid instance() {
        if (instance == null)
            instance = new TepidApi(Main.serverUrl, true)
                    .create(
                            config -> {
//                        config.setTokenRetriever(() -> Main.token);
                                return Unit.INSTANCE;
                            }
                    );
        return instance;
    }

    public static <T> T fetch(Function<ITepid, Call<T>> supplier) {
        Call<T> call = supplier.apply(instance());
        try {
            return call.execute().body();
        } catch (IOException e) {
            return null; // this should be checked
        }
    }
}
