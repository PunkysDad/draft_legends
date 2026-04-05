import { Stack } from 'expo-router';

export default function RootLayout() {
  return (
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: '#000' } }}>
      <Stack.Screen name="(tabs)" />
      <Stack.Screen name="(auth)" />
      <Stack.Screen name="draft/[matchupId]" options={{ presentation: 'modal' }} />
      <Stack.Screen name="reveal/[matchupId]" options={{ presentation: 'modal' }} />
    </Stack>
  );
}
