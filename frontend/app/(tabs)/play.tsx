import { useState } from 'react';
import {
  View,
  Text,
  Pressable,
  TextInput,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { useRouter } from 'expo-router';
import { createQuickMatch } from '../../services/matchupService';
import { autoJoinLeague, createLeague } from '../../services/leagueService';
import BrowseLeaguesModal from '../../components/BrowseLeaguesModal';

type LoadingAction = 'quickMatch' | 'autoJoin' | 'createLeague' | null;

export default function PlayScreen() {
  const router = useRouter();
  const [loadingAction, setLoadingAction] = useState<LoadingAction>(null);
  const [quickMatchError, setQuickMatchError] = useState<string | null>(null);
  const [leagueError, setLeagueError] = useState<string | null>(null);
  const [browseVisible, setBrowseVisible] = useState(false);
  const [showCreateInput, setShowCreateInput] = useState(false);
  const [leagueName, setLeagueName] = useState('');

  const handlePlayNow = async () => {
    setLoadingAction('quickMatch');
    setQuickMatchError(null);
    try {
      const matchup = await createQuickMatch();
      router.push(`/draft/${matchup.matchupId}`);
    } catch (e) {
      setQuickMatchError(e instanceof Error ? e.message : 'Failed to create matchup');
    } finally {
      setLoadingAction(null);
    }
  };

  const handleAutoJoin = async () => {
    setLoadingAction('autoJoin');
    setLeagueError(null);
    try {
      const league = await autoJoinLeague();
      router.push(`/league/${league.leagueId}`);
    } catch (e) {
      setLeagueError(e instanceof Error ? e.message : 'Failed to auto-join');
    } finally {
      setLoadingAction(null);
    }
  };

  const handleCreateLeague = async () => {
    const name = leagueName.trim();
    if (!name) return;
    setLoadingAction('createLeague');
    setLeagueError(null);
    try {
      const league = await createLeague(name);
      setShowCreateInput(false);
      setLeagueName('');
      router.push(`/league/${league.leagueId}`);
    } catch (e) {
      setLeagueError(e instanceof Error ? e.message : 'Failed to create league');
    } finally {
      setLoadingAction(null);
    }
  };

  const handleBrowseJoined = (leagueId: number) => {
    setBrowseVisible(false);
    router.push(`/league/${leagueId}`);
  };

  const busy = loadingAction !== null;

  return (
    <View style={styles.container}>
      {/* Quick Match Card */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Quick Match</Text>
        <Text style={styles.cardDesc}>1 QB · 1 RB · 1 WR · vs CPU</Text>
        <Pressable style={styles.primaryButton} onPress={handlePlayNow} disabled={busy}>
          {loadingAction === 'quickMatch' ? (
            <ActivityIndicator size="small" color="#000" />
          ) : (
            <Text style={styles.primaryButtonText}>Play Now</Text>
          )}
        </Pressable>
        {quickMatchError && <Text style={styles.errorText}>{quickMatchError}</Text>}
      </View>

      {/* Classic League Card */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Classic League</Text>
        <Text style={styles.cardDesc}>1 QB · 2 RB · 2 WR · 10-team league · 400 🪙 entry</Text>

        <Pressable
          style={styles.secondaryButton}
          onPress={() => setBrowseVisible(true)}
          disabled={busy}
        >
          <Text style={styles.secondaryButtonText}>Browse Leagues</Text>
        </Pressable>

        <Pressable style={styles.secondaryButton} onPress={handleAutoJoin} disabled={busy}>
          {loadingAction === 'autoJoin' ? (
            <ActivityIndicator size="small" color="#FFD700" />
          ) : (
            <Text style={styles.secondaryButtonText}>Auto-Join</Text>
          )}
        </Pressable>

        {showCreateInput ? (
          <View style={styles.createRow}>
            <TextInput
              style={styles.textInput}
              placeholder="League name"
              placeholderTextColor="#666"
              value={leagueName}
              onChangeText={setLeagueName}
              autoFocus
            />
            <Pressable
              style={[styles.primaryButton, { flex: 0, paddingHorizontal: 16 }]}
              onPress={handleCreateLeague}
              disabled={busy || !leagueName.trim()}
            >
              {loadingAction === 'createLeague' ? (
                <ActivityIndicator size="small" color="#000" />
              ) : (
                <Text style={styles.primaryButtonText}>Create</Text>
              )}
            </Pressable>
          </View>
        ) : (
          <Pressable
            style={styles.secondaryButton}
            onPress={() => setShowCreateInput(true)}
            disabled={busy}
          >
            <Text style={styles.secondaryButtonText}>Create League</Text>
          </Pressable>
        )}

        {leagueError && <Text style={styles.errorText}>{leagueError}</Text>}
      </View>

      <BrowseLeaguesModal
        visible={browseVisible}
        onClose={() => setBrowseVisible(false)}
        onJoined={handleBrowseJoined}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
    padding: 16,
    justifyContent: 'center',
    gap: 20,
  },
  card: {
    backgroundColor: '#1a1a1a',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#333',
    padding: 20,
  },
  cardTitle: {
    color: '#fff',
    fontSize: 22,
    fontWeight: '700',
    marginBottom: 4,
  },
  cardDesc: {
    color: '#999',
    fontSize: 14,
    marginBottom: 16,
  },
  primaryButton: {
    backgroundColor: '#FFD700',
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  primaryButtonText: {
    color: '#000',
    fontSize: 16,
    fontWeight: '700',
  },
  secondaryButton: {
    borderWidth: 1,
    borderColor: '#FFD700',
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
    marginBottom: 10,
  },
  secondaryButtonText: {
    color: '#FFD700',
    fontSize: 15,
    fontWeight: '600',
  },
  createRow: {
    flexDirection: 'row',
    gap: 10,
    alignItems: 'center',
  },
  textInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#444',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: '#fff',
    fontSize: 15,
  },
  errorText: {
    color: '#ff4444',
    fontSize: 13,
    marginTop: 10,
  },
});
