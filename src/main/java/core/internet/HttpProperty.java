package core.internet;

public class HttpProperty {

    final String key;
    final String value;

    public HttpProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
