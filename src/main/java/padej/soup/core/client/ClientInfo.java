package padej.soup.core.client;

import java.io.File;

public record ClientInfo(File clientDir, File filesDir, File configsDir) implements ClientInfoProvider {
}
