module Fastlane
  module Actions
    module SharedValues
      INSTALL_KEYSTORE_CUSTOM_VALUE = :INSTALL_KEYSTORE_CUSTOM_VALUE
    end

    class InstallKeystoreAction < Action
      def self.run(params)
        # fastlane will take care of reading in the parameter and fetching the environment variable:
        UI.message "Parameter API Token: #{params[:api_token]}"

        gitUrl = params[:gitUrl]
        keystoreName = params[:keystoreName]
        keystoreProperties = params[:keystoreProperties]
        
        tempFolder="./tmpKeystore"
        # sh "shellcommand ./path"

        cmds = [
          "pwd",
          "mkdir #{tempFolder}",
          "git clone #{gitUrl} #{tempFolder}",
          "cp #{tempFolder}/#{keystoreName} ../android/app",
          "cp #{tempFolder}/#{keystoreProperties} ../android/"
        ]
        
        result = Actions.sh(cmds.join("&"))
        UI.message("执行完毕 keystore install的操作 🚀")
        # Actions.lane_context[SharedValues::INSTALL_KEYSTORE_CUSTOM_VALUE] = "my_val"
      end

      #####################################################
      # @!group Documentation
      #####################################################

      def self.description
        "A short description with <= 80 characters of what this action does"
      end

      def self.details
        # Optional:
        # this is your chance to provide a more detailed description of this action
        "You can use this action to do cool things..."
      end

      def self.available_options
        # Define all options your action supports.

        # Below a few examples
        [
          FastlaneCore::ConfigItem.new(key: :gitUrl,
                                       env_name: "KEYSTORE_URL",
                                       description: "KEYSTORE GIT REPO URL",
                                       is_string: true, # true: verifies the input is a string, false: every kind of value
                                       default_value: false), # the default value if the user didn't provide one
          FastlaneCore::ConfigItem.new(key: :keystoreName,
                                       env_name: "KEYSTORE_NAME",
                                       description: "KEYSTORE NAME",
                                       is_string: true, # true: verifies the input is a string, false: every kind of value
                                       default_value: false), # the default value if the user didn't provide one
          FastlaneCore::ConfigItem.new(key: :keystoreProperties,
                                       env_name: "KEYSTORE_PROPERTIES_NAME",
                                       description: "KEYSTORE PROPERTIES NAME",
                                       is_string: true, # true: verifies the input is a string, false: every kind of value
                                       default_value: 'key.properties') # the default value if the user didn't provide one
        ]

      end

      def self.output
        # Define the shared values you are going to provide
        # Example
        [
          ['INSTALL_KEYSTORE_CUSTOM_VALUE', 'A description of what this value contains']
        ]
      end

      def self.return_value
        # If your method provides a return value, you can describe here what it does
      end

      def self.authors
        # So no one will ever forget your contribution to fastlane :) You are awesome btw!
        ["Your GitHub/Twitter Name"]
      end

      def self.is_supported?(platform)
        # you can do things like
        #
        #  true
        #
        #  platform == :ios
        #
        #  [:ios, :mac].include?(platform)
        #

        platform == :android
      end
    end
  end
end
