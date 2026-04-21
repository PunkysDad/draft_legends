import { useState, useEffect } from 'react';
import {
  View,
  Text,
  Image,
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
} from 'react-native';
import { Stack, useLocalSearchParams, useRouter } from 'expo-router';
import { getPlayer } from '../../services/playerService';
import type { Player } from '../../services/playerService';

const POSITION_COLORS: Record<string, string> = {
  QB: '#4A90D9',
  RB: '#4CAF50',
  WR: '#FF9800',
};

export default function PlayerDetailScreen() {
  const { playerId } = useLocalSearchParams<{ playerId: string }>();
  const router = useRouter();
  const [player, setPlayer] = useState<Player | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [photoError, setPhotoError] = useState(false);

  useEffect(() => {
    if (!playerId) return;
    getPlayer(Number(playerId))
      .then(setPlayer)
      .catch((e) => setError(e instanceof Error ? e.message : 'Failed to load player'))
      .finally(() => setLoading(false));
  }, [playerId]);

  if (loading) {
    return (
      <>
        <Stack.Screen options={{ headerShown: false }} />
        <View style={styles.center}>
          <ActivityIndicator size="large" color="#FFD700" />
        </View>
      </>
    );
  }

  if (error || !player) {
    return (
      <>
        <Stack.Screen options={{ headerShown: false }} />
        <View style={styles.center}>
          <Text style={styles.errorText}>{error ?? 'Player not found'}</Text>
          <Pressable style={styles.backButton} onPress={() => router.back()}>
            <Text style={styles.backButtonText}>Go Back</Text>
          </Pressable>
        </View>
      </>
    );
  }

  const badgeColor = POSITION_COLORS[player.position] ?? '#aaa';
  const showPhoto = !!player.photoUrl && !photoError;

  return (
    <>
      <Stack.Screen options={{ headerShown: false }} />
      <ScrollView style={styles.container} contentContainerStyle={styles.content}>
        {/* Back button */}
        <Pressable style={styles.backRow} onPress={() => router.back()}>
          <Text style={styles.backArrow}>← Back</Text>
        </Pressable>

        {/* Header */}
        <View style={styles.header}>
          <View style={styles.photoPlaceholder}>
            {showPhoto && (
              <Image
                source={{ uri: player.photoUrl }}
                style={styles.photo}
                onError={() => setPhotoError(true)}
              />
            )}
          </View>
          <View style={styles.headerInfo}>
            <Text style={styles.playerName}>{player.firstName} {player.lastName}</Text>
            <View style={[styles.badge, { backgroundColor: badgeColor + '22', borderColor: badgeColor }]}>
              <Text style={[styles.badgeText, { color: badgeColor }]}>{player.position}</Text>
            </View>
          </View>
        </View>

        {/* Career Stats Card */}
        <View style={styles.statsCard}>
          <Text style={styles.statsTitle}>Career Stats</Text>
          <StatRow label="Avg Fantasy Pts" value={player.salary.toFixed(2)} />
          <StatRow label="Salary Cap Value" value={player.salary.toFixed(2)} />
          <StatRow
            label="Volatility"
            value={`${player.volatility.toFixed(2)}`}
            detail="risk/reward indicator"
          />
          <StatRow label="Seasons Played" value={String(player.seasonsPlayed)} />
          <StatRow label="Career TDs" value={String(player.totalTouchdowns)} />
          {player.totalInterceptions !== null && (
            <StatRow label="Career INTs" value={String(player.totalInterceptions)} />
          )}
        </View>
      </ScrollView>
    </>
  );
}

function StatRow({ label, value, detail }: { label: string; value: string; detail?: string }) {
  return (
    <View style={styles.statRow}>
      <View style={styles.statLabelGroup}>
        <Text style={styles.statLabel}>{label}</Text>
        {detail && <Text style={styles.statDetail}>{detail}</Text>}
      </View>
      <Text style={styles.statValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  content: {
    padding: 16,
    paddingBottom: 40,
  },
  center: {
    flex: 1,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  backRow: {
    marginBottom: 16,
  },
  backArrow: {
    color: '#FFD700',
    fontSize: 15,
    fontWeight: '600',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    marginBottom: 24,
  },
  photoPlaceholder: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: '#333',
    overflow: 'hidden',
  },
  photo: {
    width: '100%',
    height: '100%',
  },
  headerInfo: {
    flex: 1,
    gap: 8,
  },
  playerName: {
    color: '#fff',
    fontSize: 22,
    fontWeight: '700',
  },
  badge: {
    alignSelf: 'flex-start',
    borderWidth: 1,
    borderRadius: 6,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '700',
  },
  statsCard: {
    backgroundColor: '#1a1a1a',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#333',
    padding: 20,
  },
  statsTitle: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 16,
  },
  statRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#2a2a2a',
  },
  statLabelGroup: {
    flex: 1,
  },
  statLabel: {
    color: '#ccc',
    fontSize: 14,
  },
  statDetail: {
    color: '#666',
    fontSize: 11,
    marginTop: 2,
  },
  statValue: {
    color: '#FFD700',
    fontSize: 16,
    fontWeight: '700',
  },
  errorText: {
    color: '#ff4444',
    fontSize: 15,
    marginBottom: 16,
    textAlign: 'center',
  },
  backButton: {
    borderWidth: 1,
    borderColor: '#FFD700',
    borderRadius: 8,
    paddingVertical: 10,
    paddingHorizontal: 24,
  },
  backButtonText: {
    color: '#FFD700',
    fontSize: 15,
    fontWeight: '600',
  },
});
