export default {
  expo: {
    name: "Legends Clash",
    slug: "legends-clash",
    version: "1.0.0",
    orientation: "portrait",
    scheme: "com.draftlegends.app",
    userInterfaceStyle: "dark",
    ios: {
      supportsTablet: false,
      bundleIdentifier: "com.draftlegends.app"
    },
    android: {
      adaptiveIcon: {
        backgroundColor: "#000000"
      },
      package: "com.draftlegends.app"
    },
    plugins: ["expo-router"],
    experiments: {
      typedRoutes: true
    }
  }
};
