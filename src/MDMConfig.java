import config.ConfigInfo;

/**
 * Config class
 */
public enum MDMConfig implements ConfigInfo
{
    CLIENT_PORT(56987),
    INTECHOS_PORT(56990),
    SNMP_PORT(56991)

    ;

    private MDMConfig(Object defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    private Object defaultValue;

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isMutable() {
        return true;
    }
}
