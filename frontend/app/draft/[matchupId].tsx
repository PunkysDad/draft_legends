import { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  Pressable,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useAuth } from '../../context/AuthContext';
import { getMatchup, submitPick } from '../../services/matchupService';
import type { Matchup } from '../../services/matchupService';
import { getPlayers } from '../../services/playerService';
import type { Player } from '../../services/playerService';
import PlayerCard from '../../components/PlayerCard';
import CountdownModal from '../../components/CountdownModal';

type PositionFilter = 'All' | 'QB' | 'RB' | 'WR';

const SLOT_SEQUENCE: Array<'QB' | 'RB' | 'WR'> = ['QB', 'RB', 'WR'];

function getNextSlot(picksCount: number): 'QB' | 'RB' | 'WR' | null {
  if (picksCount >= 6) return null;
  // User picks on even counts: 0 → QB, 2 → RB, 4 → WR
  if (picksCount % 2 === 0) return SLOT_SEQUENCE[picksCount / 2];
  return null; // CPU's turn
}

export default function DraftScreen() {
  const { matchupId } = useLocalSearchParams<{ matchupId: string }>();
  const router = useRouter();
  const { user } = useAuth();

  const [matchup, setMatchup] = useState<Matchup | null>(null);
  const [allPlayers, setAllPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [picking, setPicking] = useState(false);
  const [pickError, setPickError] = useState<string | null>(null);
  const [positionFilter, setPositionFilter] = useState<PositionFilter>('All');
  const [showCountdown, setShowCountdown] = useState(false);
  const [completedMatchup, setCompletedMatchup] = useState<Matchup | null>(null);

  useEffect(() => {
    Promise.all([
      getMatchup(Number(matchupId)),
      getPlayers(),
    ])
      .then(([m, p]) => {
        setMatchup(m);
        setAllPlayers(p);
      })
      .catch((e) => setError(e instanceof Error ? e.message : 'Failed to load draft'))
      .finally(() => setLoading(false));
  }, [matchupId]);

  const handlePick = useCallback(
    async (player: Player) => {
      if (!matchup || picking) return;
      const nextSlot = getNextSlot(matchup.picks.length);
      if (!nextSlot) return;

      setPicking(true);
      setPickError(null);
      try {
        const updated = await submitPick(Number(matchupId), player.playerId, nextSlot);
        setMatchup(updated);
        if (updated.status === 'COMPLETE') {
          setCompletedMatchup(updated);
          setShowCountdown(true);
        }
      } catch (e) {
        setPickError(e instanceof Error ? e.message : 'Failed to submit pick');
      } finally {
        setPicking(false);
      }
    },
    [matchup, matchupId, picking],
  );

  const handleCountdownComplete = useCallback(() => {
    setShowCountdown(false);
    if (completedMatchup) {
      router.replace({
        pathname: `/reveal/${matchupId}`,
        params: { matchup: JSON.stringify(completedMatchup) },
      });
    }
  }, [completedMatchup, matchupId, router]);

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#FFD700" />
      </View>
    );
  }

  if (error || !matchup) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error ?? 'Matchup not found'}</Text>
        <Pressable style={styles.backButton} onPress={() => router.back()}>
          <Text style={styles.backButtonText}>Go Back</Text>
        </Pressable>
      </View>
    );
  }

  const userPicks = matchup.picks.filter((p) => p.userId === user?.userId);
  const cpuPicks = matchup.picks.filter((p) => p.userId !== user?.userId);
  const draftedIds = new Set(matchup.picks.map((p) => p.player.playerId));
  const nextSlot = getNextSlot(matchup.picks.length);
  const isUserTurn = nextSlot !== null;

  const userSalary = userPicks.reduce((sum, p) => sum + p.player.salary, 0);

  const availablePlayers = allPlayers.filter(
    (p) =>
      !draftedIds.has(p.playerId) &&
      (positionFilter === 'All' || p.position === positionFilter),
  );

  const slotLabel =
    matchup.picks.length >= 6
      ? 'Draft complete'
      : isUserTurn
        ? `Pick your ${nextSlot}`
        : 'CPU is picking...';

  return (
    <>
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Draft Room</Text>
          <Text style={styles.salaryText}>
            💰 {userSalary.toFixed(2)} / {matchup.salaryCapPerTeam}
          </Text>
        </View>

        {/* Slot indicator */}
        <View style={styles.slotIndicator}>
          <Text style={[styles.slotLabel, !isUserTurn && styles.slotLabelMuted]}>
            {slotLabel}
          </Text>
        </View>

        {/* Roster panels */}
        <View style={styles.rosterRow}>
          <View style={styles.rosterPanel}>
            <Text style={styles.rosterTitle}>Your Picks</Text>
            {userPicks.length === 0 ? (
              <Text style={styles.rosterEmpty}>—</Text>
            ) : (
              userPicks.map((pick) => (
                <Text key={pick.pickNumber} style={styles.rosterItem}>
                  {pick.slotType}: {pick.player.firstName} {pick.player.lastName}
                </Text>
              ))
            )}
          </View>
          <View style={styles.rosterDivider} />
          <View style={styles.rosterPanel}>
            <Text style={styles.rosterTitle}>CPU Picks</Text>
            {cpuPicks.length === 0 ? (
              <Text style={styles.rosterEmpty}>—</Text>
            ) : (
              cpuPicks.map((pick) => (
                <Text key={pick.pickNumber} style={styles.rosterItem}>
                  {pick.slotType}: {pick.player.firstName} {pick.player.lastName}
                </Text>
              ))
            )}
          </View>
        </View>

        {/* Filter bar */}
        <View style={styles.filterBar}>
          {(['All', 'QB', 'RB', 'WR'] as PositionFilter[]).map((pos) => (
            <Pressable
              key={pos}
              style={[styles.chip, positionFilter === pos && styles.chipActive]}
              onPress={() => setPositionFilter(pos)}
            >
              <Text style={[styles.chipText, positionFilter === pos && styles.chipTextActive]}>
                {pos}
              </Text>
            </Pressable>
          ))}
        </View>

        {pickError && <Text style={styles.pickError}>{pickError}</Text>}

        {/* Player pool */}
        <FlatList
          data={availablePlayers}
          keyExtractor={(item) => String(item.playerId)}
          renderItem={({ item }) => (
            <PlayerCard
              player={item}
              onPress={isUserTurn && !picking ? () => handlePick(item) : undefined}
            />
          )}
          contentContainerStyle={styles.list}
          ListEmptyComponent={<Text style={styles.emptyText}>No players available</Text>}
        />

        {/* Pick loading overlay */}
        {picking && (
          <View style={styles.pickingOverlay}>
            <ActivityIndicator size="large" color="#FFD700" />
            <Text style={styles.pickingText}>Submitting pick...</Text>
          </View>
        )}
      </View>

      <CountdownModal visible={showCountdown} onComplete={handleCountdownComplete} />
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  center: {
    flex: 1,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#222',
  },
  headerTitle: {
    color: '#FFD700',
    fontSize: 20,
    fontWeight: '700',
  },
  salaryText: {
    color: '#ccc',
    fontSize: 14,
  },
  slotIndicator: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: '#111',
  },
  slotLabel: {
    color: '#FFD700',
    fontSize: 16,
    fontWeight: '700',
    textAlign: 'center',
  },
  slotLabelMuted: {
    color: '#666',
  },
  rosterRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: '#111',
    borderBottomWidth: 1,
    borderBottomColor: '#222',
  },
  rosterPanel: {
    flex: 1,
  },
  rosterDivider: {
    width: 1,
    backgroundColor: '#333',
    marginHorizontal: 12,
  },
  rosterTitle: {
    color: '#888',
    fontSize: 11,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  rosterItem: {
    color: '#ddd',
    fontSize: 12,
    lineHeight: 18,
  },
  rosterEmpty: {
    color: '#444',
    fontSize: 12,
  },
  filterBar: {
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: '#0a0a0a',
    borderBottomWidth: 1,
    borderBottomColor: '#1a1a1a',
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#444',
  },
  chipActive: {
    backgroundColor: '#FFD700',
    borderColor: '#FFD700',
  },
  chipText: {
    color: '#ccc',
    fontSize: 13,
    fontWeight: '600',
  },
  chipTextActive: {
    color: '#000',
  },
  list: {
    padding: 16,
    paddingBottom: 32,
  },
  pickError: {
    color: '#ff4444',
    fontSize: 13,
    paddingHorizontal: 16,
    paddingTop: 8,
    textAlign: 'center',
  },
  emptyText: {
    color: '#666',
    fontSize: 14,
    textAlign: 'center',
    marginTop: 32,
  },
  pickingOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 12,
  },
  pickingText: {
    color: '#FFD700',
    fontSize: 16,
    fontWeight: '600',
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
