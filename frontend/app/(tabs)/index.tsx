import { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  Pressable,
  ActivityIndicator,
  RefreshControl,
  StyleSheet,
} from 'react-native';
import { useRouter } from 'expo-router';
import MatchupCard from '../../components/MatchupCard';
import { getActiveMatchups, createQuickMatch, type MatchupSummary } from '../../services/matchupService';

export default function HomeScreen() {
  const router = useRouter();
  const [matchups, setMatchups] = useState<MatchupSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const fetchMatchups = useCallback(async () => {
    try {
      const data = await getActiveMatchups();
      setMatchups(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load matchups');
    }
  }, []);

  useEffect(() => {
    fetchMatchups().finally(() => setLoading(false));
  }, [fetchMatchups]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await fetchMatchups();
    setRefreshing(false);
  }, [fetchMatchups]);

  const handlePlayNow = async () => {
    setCreating(true);
    setCreateError(null);
    try {
      const matchup = await createQuickMatch();
      await fetchMatchups();
      router.push(`/draft/${matchup.matchupId}`);
    } catch (e) {
      setCreateError(e instanceof Error ? e.message : 'Failed to create matchup');
    } finally {
      setCreating(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#FFD700" />
      </View>
    );
  }

  if (error && matchups.length === 0) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>{error}</Text>
        <Pressable style={styles.retryButton} onPress={() => { setLoading(true); fetchMatchups().finally(() => setLoading(false)); }}>
          <Text style={styles.retryText}>Retry</Text>
        </Pressable>
      </View>
    );
  }

  const quickMatches = matchups.filter((m) => m.type === 'QUICK_MATCH');
  const classicLeagues = matchups.filter((m) => m.type === 'CLASSIC_LEAGUE');

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#FFD700" />}
    >
      {/* Quick Match Section */}
      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>Quick Match</Text>
        <Pressable style={styles.playNowButton} onPress={handlePlayNow} disabled={creating}>
          {creating ? (
            <ActivityIndicator size="small" color="#000" />
          ) : (
            <Text style={styles.playNowText}>Play Now</Text>
          )}
        </Pressable>
      </View>
      {createError && <Text style={styles.createError}>{createError}</Text>}
      {quickMatches.length === 0 ? (
        <Text style={styles.emptyText}>No active Quick Matches</Text>
      ) : (
        quickMatches.map((m) => <MatchupCard key={m.matchupId} matchup={m} />)
      )}

      {/* Classic League Section */}
      <View style={[styles.sectionHeader, { marginTop: 28 }]}>
        <Text style={styles.sectionTitle}>Classic League</Text>
      </View>
      {classicLeagues.length === 0 ? (
        <Text style={styles.emptyText}>No active leagues</Text>
      ) : (
        classicLeagues.map((m) => <MatchupCard key={m.matchupId} matchup={m} />)
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  content: {
    padding: 16,
    paddingBottom: 32,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#000',
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  sectionTitle: {
    color: '#fff',
    fontSize: 20,
    fontWeight: '700',
  },
  playNowButton: {
    backgroundColor: '#FFD700',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 8,
    minWidth: 90,
    alignItems: 'center',
  },
  playNowText: {
    color: '#000',
    fontSize: 14,
    fontWeight: '700',
  },
  createError: {
    color: '#ff4444',
    fontSize: 13,
    marginBottom: 10,
  },
  emptyText: {
    color: '#666',
    fontSize: 14,
    fontStyle: 'italic',
    marginBottom: 8,
  },
  errorText: {
    color: '#ff4444',
    fontSize: 16,
    marginBottom: 16,
    textAlign: 'center',
  },
  retryButton: {
    backgroundColor: '#FFD700',
    borderRadius: 8,
    paddingHorizontal: 24,
    paddingVertical: 10,
  },
  retryText: {
    color: '#000',
    fontSize: 15,
    fontWeight: '700',
  },
});
