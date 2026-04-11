import { View, Text } from 'react-native';
import { useLocalSearchParams } from 'expo-router';

export default function PlayerDetailScreen() {
  const { playerId } = useLocalSearchParams();
  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#000' }}>
      <Text style={{ color: '#fff', fontSize: 18 }}>Player Detail — {playerId}</Text>
    </View>
  );
}
