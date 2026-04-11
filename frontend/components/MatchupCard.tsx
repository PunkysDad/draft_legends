import { Pressable, View, Text, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import type { MatchupSummary } from '../services/matchupService';

export default function MatchupCard({ matchup }: { matchup: MatchupSummary }) {
  const router = useRouter();

  const handlePress = () => {
    if (matchup.status === 'DRAFTING') {
      router.push(`/draft/${matchup.matchupId}`);
    } else if (matchup.status === 'COMPLETE') {
      router.push(`/reveal/${matchup.matchupId}`);
    }
  };

  const badgeColor = matchup.status === 'DRAFTING' ? '#FFD700' : '#4CAF50';

  return (
    <Pressable style={styles.card} onPress={handlePress}>
      <Text style={styles.opponent} numberOfLines={1}>{matchup.opponentName}</Text>
      <Text style={styles.score}>
        {matchup.myScore.toFixed(1)} — {matchup.opponentScore.toFixed(1)}
      </Text>
      <View style={[styles.badge, { backgroundColor: badgeColor + '22', borderColor: badgeColor }]}>
        <Text style={[styles.badgeText, { color: badgeColor }]}>{matchup.status}</Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#1a1a1a',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#333',
    padding: 14,
    marginBottom: 10,
  },
  opponent: {
    flex: 1,
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
  },
  score: {
    color: '#ccc',
    fontSize: 14,
    marginHorizontal: 12,
  },
  badge: {
    borderWidth: 1,
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  badgeText: {
    fontSize: 11,
    fontWeight: '700',
  },
});
