package typeusebug;

import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TheSource {

  public @Nullable List<@Nullable String> theMethod() {
    throw new UnsupportedOperationException();
  }

}
