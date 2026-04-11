import { useEffect, useState, useCallback } from 'react';
import {
  Modal,
  View,
  Text,
  FlatList,
  Pressable,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { getOpenLeagues, joinLeague, type League } from '../services/leagueService';

interface Props {
  visible: boolean;
  onClose: () => void;
  onJoined: (leagueId: number) => void;
}

export default function BrowseLeaguesModal({ visible, onClose, onJoined }: Props) {
  const [leagues, setLeagues] = useState<League[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [joiningId, setJoiningId] = useState<number | null>(null);

  const fetchLeagues = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getOpenLeagues();
      setLeagues(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load leagues');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (visible) fetchLeagues();
  }, [visible, fetchLeagues]);

  const handleJoin = async (leagueId: number) => {
    setJoiningId(leagueId);
    try {
      await joinLeague(leagueId);
      onJoined(leagueId);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to join league');
    } finally {
      setJoiningId(null);
    }
  };

  const renderItem = ({ item }: { item: League }) => (
    <View style={styles.row}>
      <View style={styles.rowInfo}>
        <Text style={styles.leagueName} numberOfLines={1}>{item.name}</Text>
        <Text style={styles.leagueMeta}>
          {item.currentTeams} / {item.maxTeams} teams · {item.entryFee} 🪙
        </Text>
      </View>
      <Pressable
        style={styles.joinButton}
        onPress={() => handleJoin(item.leagueId)}
        disabled={joiningId !== null}
      >
        {joiningId === item.leagueId ? (
          <ActivityIndicator size="small" color="#000" />
        ) : (
          <Text style={styles.joinText}>Join</Text>
        )}
      </Pressable>
    </View>
  );

  return (
    <Modal visible={visible} animationType="slide" transparent>
      <View style={styles.overlay}>
        <View style={styles.container}>
          <View style={styles.header}>
            <Text style={styles.title}>Open Leagues</Text>
            <Pressable onPress={onClose}>
              <Text style={styles.closeButton}>✕</Text>
            </Pressable>
          </View>

          {loading ? (
            <ActivityIndicator size="large" color="#FFD700" style={styles.centered} />
          ) : error ? (
            <View style={styles.centered}>
              <Text style={styles.errorText}>{error}</Text>
              <Pressable style={styles.retryButton} onPress={fetchLeagues}>
                <Text style={styles.retryText}>Retry</Text>
              </Pressable>
            </View>
          ) : leagues.length === 0 ? (
            <Text style={styles.emptyText}>No open leagues right now — create one!</Text>
          ) : (
            <FlatList
              data={leagues}
              keyExtractor={(item) => String(item.leagueId)}
              renderItem={renderItem}
            />
          )}
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'flex-end',
  },
  container: {
    backgroundColor: '#111',
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    maxHeight: '75%',
    padding: 16,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  title: {
    color: '#fff',
    fontSize: 20,
    fontWeight: '700',
  },
  closeButton: {
    color: '#999',
    fontSize: 22,
    padding: 4,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#1a1a1a',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#333',
    padding: 14,
    marginBottom: 10,
  },
  rowInfo: {
    flex: 1,
  },
  leagueName: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
    marginBottom: 2,
  },
  leagueMeta: {
    color: '#999',
    fontSize: 13,
  },
  joinButton: {
    backgroundColor: '#FFD700',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 8,
    minWidth: 60,
    alignItems: 'center',
  },
  joinText: {
    color: '#000',
    fontSize: 14,
    fontWeight: '700',
  },
  centered: {
    paddingVertical: 40,
    alignItems: 'center',
  },
  errorText: {
    color: '#ff4444',
    fontSize: 14,
    marginBottom: 12,
    textAlign: 'center',
  },
  retryButton: {
    backgroundColor: '#FFD700',
    borderRadius: 8,
    paddingHorizontal: 20,
    paddingVertical: 8,
  },
  retryText: {
    color: '#000',
    fontSize: 14,
    fontWeight: '700',
  },
  emptyText: {
    color: '#666',
    fontSize: 14,
    fontStyle: 'italic',
    textAlign: 'center',
    paddingVertical: 40,
  },
});
