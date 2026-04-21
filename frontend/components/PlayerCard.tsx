import { Pressable, View, Text, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import type { Player } from '../services/playerService';

const POSITION_COLORS: Record<string, string> = {
  QB: '#4A90D9',
  RB: '#4CAF50',
  WR: '#FF9800',
};

export default function PlayerCard({ player, onPress }: { player: Player; onPress?: () => void }) {
  const router = useRouter();
  const badgeColor = POSITION_COLORS[player.position] ?? '#aaa';

  return (
    <Pressable
      style={styles.card}
      onPress={onPress ?? (() => router.push(`/players/${player.playerId}`))}
    >
      <View style={styles.info}>
        <View style={styles.nameRow}>
          <Text style={styles.name}>{player.firstName} {player.lastName}</Text>
          <View style={[styles.badge, { backgroundColor: badgeColor + '22', borderColor: badgeColor }]}>
            <Text style={[styles.badgeText, { color: badgeColor }]}>{player.position}</Text>
          </View>
        </View>
      </View>
      <View style={styles.stats}>
        <Text style={styles.salary}>💰 {player.salary.toFixed(2)}</Text>
        <Text style={styles.volatility}>⚡ {player.volatility.toFixed(1)}</Text>
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
  info: {
    flex: 1,
  },
  nameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  name: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '700',
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
  stats: {
    alignItems: 'flex-end',
    gap: 4,
  },
  salary: {
    color: '#FFD700',
    fontSize: 14,
    fontWeight: '600',
  },
  volatility: {
    color: '#999',
    fontSize: 12,
  },
});
