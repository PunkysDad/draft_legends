import { View, Text } from 'react-native';
import { useLocalSearchParams } from 'expo-router';

export default function DraftScreen() {
  const { matchupId } = useLocalSearchParams();
  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#000' }}>
      <Text style={{ color: '#FFD700', fontSize: 22, fontWeight: 'bold', marginBottom: 8 }}>Draft Room</Text>
      <Text style={{ color: '#999', fontSize: 16 }}>Matchup #{matchupId}</Text>
      <Text style={{ color: '#666', fontSize: 14, marginTop: 16 }}>Coming Soon</Text>
    </View>
  );
}
