import { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  Pressable,
  ActivityIndicator,
  StyleSheet,
  RefreshControl,
} from 'react-native';
import { getPlayers } from '../../services/playerService';
import type { Player } from '../../services/playerService';
import PlayerCard from '../../components/PlayerCard';

type PositionFilter = 'All' | 'QB' | 'RB' | 'WR';
type SortOption = 'salary' | 'volatility' | 'lastName';

const POSITIONS: PositionFilter[] = ['All', 'QB', 'RB', 'WR'];
const SORT_OPTIONS: { label: string; value: SortOption }[] = [
  { label: 'Salary', value: 'salary' },
  { label: 'Volatility', value: 'volatility' },
  { label: 'A–Z', value: 'lastName' },
];

export default function PlayersScreen() {
  const [players, setPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [position, setPosition] = useState<PositionFilter>('All');
  const [sortBy, setSortBy] = useState<SortOption>('salary');

  const load = useCallback(async (pos: PositionFilter, sort: SortOption) => {
    setError(null);
    try {
      const data = await getPlayers({
        ...(pos !== 'All' ? { position: pos } : {}),
        sortBy: sort,
        sortDir: sort === 'lastName' ? 'asc' : 'desc',
      });
      setPlayers(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load players');
    }
  }, []);

  useEffect(() => {
    load('All', 'salary').finally(() => setLoading(false));
  }, [load]);

  const handlePositionChange = (pos: PositionFilter) => {
    setPosition(pos);
    load(pos, sortBy);
  };

  const handleSortChange = (sort: SortOption) => {
    setSortBy(sort);
    load(position, sort);
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await load(position, sortBy);
    setRefreshing(false);
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#FFD700" />
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error}</Text>
        <Pressable style={styles.retryButton} onPress={() => load(position, sortBy)}>
          <Text style={styles.retryButtonText}>Retry</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.filterBar}>
        <View style={styles.filterRow}>
          {POSITIONS.map((pos) => (
            <Pressable
              key={pos}
              style={[styles.chip, position === pos && styles.chipActive]}
              onPress={() => handlePositionChange(pos)}
            >
              <Text style={[styles.chipText, position === pos && styles.chipTextActive]}>
                {pos}
              </Text>
            </Pressable>
          ))}
        </View>
        <View style={styles.filterRow}>
          {SORT_OPTIONS.map(({ label, value }) => (
            <Pressable
              key={value}
              style={[styles.sortChip, sortBy === value && styles.sortChipActive]}
              onPress={() => handleSortChange(value)}
            >
              <Text style={[styles.sortChipText, sortBy === value && styles.sortChipTextActive]}>
                {label}
              </Text>
            </Pressable>
          ))}
        </View>
      </View>

      <FlatList
        data={players}
        keyExtractor={(item) => String(item.playerId)}
        renderItem={({ item }) => <PlayerCard player={item} />}
        contentContainerStyle={styles.list}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#FFD700" />
        }
        ListEmptyComponent={<Text style={styles.emptyText}>No players found</Text>}
      />
    </View>
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
  filterBar: {
    backgroundColor: '#111',
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#222',
  },
  filterRow: {
    flexDirection: 'row',
    gap: 8,
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
  sortChip: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#333',
  },
  sortChipActive: {
    borderColor: '#FFD700',
  },
  sortChipText: {
    color: '#888',
    fontSize: 13,
  },
  sortChipTextActive: {
    color: '#FFD700',
    fontWeight: '600',
  },
  list: {
    padding: 16,
  },
  errorText: {
    color: '#ff4444',
    fontSize: 15,
    marginBottom: 16,
    textAlign: 'center',
  },
  retryButton: {
    borderWidth: 1,
    borderColor: '#FFD700',
    borderRadius: 8,
    paddingVertical: 10,
    paddingHorizontal: 24,
  },
  retryButtonText: {
    color: '#FFD700',
    fontSize: 15,
    fontWeight: '600',
  },
  emptyText: {
    color: '#666',
    fontSize: 15,
    textAlign: 'center',
    marginTop: 40,
  },
});
