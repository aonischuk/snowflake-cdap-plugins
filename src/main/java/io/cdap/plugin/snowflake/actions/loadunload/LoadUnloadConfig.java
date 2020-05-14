/*
 * Copyright © 2020 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.plugin.snowflake.actions.loadunload;

import com.google.common.base.Strings;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.validation.InvalidConfigPropertyException;
import io.cdap.plugin.snowflake.common.BaseSnowflakeConfig;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Config for load and unload from Snowflake actions.
 */
public abstract class LoadUnloadConfig extends BaseSnowflakeConfig {
  private static final String PROPERTY_USE_CLOUD_PROVIDER_PARAMETERS = "useCloudProviderParameters";
  private static final String PROPERTY_FILE_FORMAT_FILTERING_POLICY = "fileFormatFilteringPolicy";
  private static final String PROPERTY_CLOUD_PROVIDER = "cloudProvider";
  private static final String PROPERTY_ENCRYPTION_TYPE = "encryptionType";
  private static final String PROPERTY_FORMAT_TYPE = "formatType";
  /**
   * Cloud provider parameters
   */
  @Name(PROPERTY_USE_CLOUD_PROVIDER_PARAMETERS)
  @Description("If true, plugin will use specified Cloud Provider Parameters.")
  @Macro
  private Boolean useCloudProviderParameters;

  @Name(PROPERTY_CLOUD_PROVIDER)
  @Description("Name of cloud provider. Possible values: GCP, AWS, Microsoft Azure")
  @Macro
  @Nullable
  private String cloudProvider;

  @Description("Name of the storage integration used to delegate authentication responsibility for external " +
    "cloud storage to a Snowflake identity and access management (IAM) entity.")
  @Macro
  @Nullable
  private String storageIntegration;

  @Description("Key Id for connecting to AWS and accessing the private/protected S3 bucket " +
    "where the files to load are staged. ")
  @Macro
  @Nullable
  private String awsKeyId;

  @Description("Secret Key for connecting to AWS and accessing the private/protected S3 " +
    "bucket where the files to load are staged.")
  @Macro
  @Nullable
  private String awsSecretKey;

  @Description("Token for connecting to AWS and accessing the private/protected S3 bucket " +
    "where the files to load are staged.")
  @Macro
  @Nullable
  private String awsToken;

  @Description("Shared access signature awsToken for connecting to Azure and accessing the " +
    "private/protected container where the files containing data are staged. Credentials are generated by Azure.")
  @Macro
  @Nullable
  private String azureSasToken;

  @Description("If true, plugin will perform loading from encrypted files.")
  @Macro
  @Nullable
  private Boolean filesEncrypted;

  @Name(PROPERTY_ENCRYPTION_TYPE)
  @Description("Encryption type used.")
  @Macro
  @Nullable
  private String encryptionType;

  @Description("Client-side master key that was used to encrypt the files in the bucket. The master key must be " +
    "a 128-bit or 256-bit key in Base64-encoded form. Snowflake requires this key to decrypt encrypted files " +
    "in the bucket and extract data for loading.")
  @Macro
  @Nullable
  private String masterKey;

  @Description("AWS KMS Key Id.")
  @Macro
  @Nullable
  private String kmsKeyId;


  /**
   * File format
   */
  @Name(PROPERTY_FILE_FORMAT_FILTERING_POLICY)
  @Description("Type of filtering of the data files to load.")
  @Macro
  private String fileFormatFilteringPolicy;

  @Description("Existing named file format to use for loading data into the table. The named file format " +
    "determines the format type (CSV, JSON, etc.), as well as any other format options, for the data files.")
  @Macro
  @Nullable
  private String formatName;

  @Name(PROPERTY_FORMAT_TYPE)
  @Description("Type of files to load into the table. If a format type is specified, " +
    "then additional format-specific options can be specified.")
  @Macro
  @Nullable
  private String formatType;

  @Description("Format-specific options separated by blank spaces or new lines.")
  @Macro
  @Nullable
  private String formatTypeOptions;

  @Description("One or more copy options separated by blank spaces or new lines.")
  @Macro
  @Nullable
  private String copyOptions;


  public LoadUnloadConfig(String accountName, String database,
                          String schemaName, String username, String password,
                          @Nullable Boolean keyPairEnabled, @Nullable String path,
                          @Nullable String passphrase, @Nullable Boolean oauth2Enabled, @Nullable String clientId,
                          @Nullable String clientSecret, @Nullable String refreshToken,
                          @Nullable String connectionArguments) {
    super(accountName, database, schemaName, username, password, keyPairEnabled, path, passphrase, oauth2Enabled,
          clientId, clientSecret, refreshToken, connectionArguments);
  }

  public Boolean getUseCloudProviderParameters() {
    return useCloudProviderParameters;
  }

  public CloudProvider getCloudProvider() {
    CloudProvider value = getEnumValueByString(CloudProvider.class, cloudProvider, PROPERTY_CLOUD_PROVIDER);
    return (value == null) ? CloudProvider.NONE : value;
  }

  @Nullable
  public String getStorageIntegration() {
    return storageIntegration;
  }

  @Nullable
  public String getAwsKeyId() {
    return awsKeyId;
  }

  @Nullable
  public String getAwsSecretKey() {
    return awsSecretKey;
  }

  @Nullable
  public String getAwsToken() {
    return awsToken;
  }

  @Nullable
  public String getAzureSasToken() {
    return azureSasToken;
  }

  @Nullable
  public Boolean getFilesEncrypted() {
    return filesEncrypted;
  }

  public EncryptionType getEncryptionType() {
    EncryptionType value = getEnumValueByString(EncryptionType.class, encryptionType, PROPERTY_ENCRYPTION_TYPE);
    return (value == null) ? EncryptionType.NONE : value;
  }

  @Nullable
  public String getMasterKey() {
    return masterKey;
  }

  @Nullable
  public String getKmsKeyId() {
    return kmsKeyId;
  }

  public FileFormatFilteringPolicy getFileFormatFilteringPolicy() {
    return getEnumValueByString(FileFormatFilteringPolicy.class, fileFormatFilteringPolicy,
                                PROPERTY_FILE_FORMAT_FILTERING_POLICY);
  }

  @Nullable
  public String getFormatName() {
    return formatName;
  }

  @Nullable
  public String getFormatType() {
    return formatType;
  }

  public String getFormatTypeOptions() {
    String formatTypeOptions = (this.formatTypeOptions == null) ? "" : this.formatTypeOptions;
    return formatTypeOptions.replace(",", " ").replace(":", "=");
  }

  public String getCopyOptions() {
    String copyOptions = (this.copyOptions == null) ? "" : this.copyOptions;
    return copyOptions.replace(",", " ").replace(":", "=");
  }

  @Override
  public void validate(FailureCollector collector) {
    super.validate(collector);

    if (getFileFormatFilteringPolicy().equals(FileFormatFilteringPolicy.BY_FILE_TYPE) &&
      Strings.isNullOrEmpty(formatType)) {
      collector.addFailure("'Format Type' property is not set.", null)
        .withConfigProperty(PROPERTY_FORMAT_TYPE);
    }

    if (!containsMacro(PROPERTY_USE_CLOUD_PROVIDER_PARAMETERS) && useCloudProviderParameters) {
      if (!containsMacro(PROPERTY_USE_CLOUD_PROVIDER_PARAMETERS) &&
        (Strings.isNullOrEmpty(cloudProvider) || getCloudProvider().equals(CloudProvider.NONE))) {
        collector.addFailure("'Cloud Provider' property is not set.", null)
          .withConfigProperty(PROPERTY_CLOUD_PROVIDER);
      }

      if (filesEncrypted != null && filesEncrypted && (Strings.isNullOrEmpty(encryptionType)
        || getEncryptionType().equals(EncryptionType.NONE))) {
        collector.addFailure("'Encryption Type' property is not set.", null)
          .withConfigProperty(PROPERTY_ENCRYPTION_TYPE);
      }
    }
  }

  protected static <T extends EnumWithValue> T
  getEnumValueByString(Class<T> enumClass, String stringValue, String propertyName) {
    if (stringValue == null) {
      return null;
    }

    return Stream.of(enumClass.getEnumConstants())
      .filter(keyType -> keyType.getValue().equalsIgnoreCase(stringValue))
      .findAny()
      .orElseThrow(() -> new InvalidConfigPropertyException(
        String.format("Unsupported value for '%s': '%s'", propertyName, stringValue), propertyName));
  }
}
