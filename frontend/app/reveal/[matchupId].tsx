import { useState, useMemo } from 'react';
import {
  View,
  Text,
  Pressable,
  ScrollView,
  StyleSheet,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useAuth } from '../../context/AuthContext';
import type { Matchup, GameResult } from '../../services/matchupService';
import PlayerRevealModal from '../../components/PlayerRevealModal';

const POSITION_COLORS: Record<string, string> = {
  QB: '#4A90D9',
  RB: '#4CAF50',
  WR: '#FF9800',
};

export default function RevealScreen() {
  const { matchup: matchupParam } = useLocalSearchParams<{ matchup: string }>();
  const router = useRouter();
  const { user } = useAuth();

  const matchup: Matchup | null = useMemo(() => {
    try {
      return matchupParam ? (JSON.parse(matchupParam) as Matchup) : null;
    } catch {
      return null;
    }
  }, [matchupParam]);

  // Build interleaved reveal sequence: [user0, cpu0, user1, cpu1, user2, cpu2]
  const revealSequence: Array<{ result: GameResult; isUser: boolean }> = useMemo(() => {
    if (!matchup || !user) return [];
    const userResults = matchup.gameResults.filter((r) => r.userId === user.userId);
    const cpuResults = matchup.gameResults.filter((r) => r.userId !== user.userId);
    const maxLen = Math.max(userResults.length, cpuResults.length);
    const seq: Array<{ result: GameResult; isUser: boolean }> = [];
    for (let i = 0; i < maxLen; i++) {
      if (userResults[i]) seq.push({ result: userResults[i], isUser: true });
      if (cpuResults[i]) seq.push({ result: cpuResults[i], isUser: false });
    }
    return seq;
  }, [matchup, user]);

  const [currentRevealIndex, setCurrentRevealIndex] = useState(0);
  const [revealComplete, setRevealComplete] = useState(false);
  const [reviewResult, setReviewResult] = useState<{ result: GameResult; isUser: boolean } | null>(null);

  if (!matchup || !user) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>Could not load results</Text>
        <Pressable style={styles.backButton} onPress={() => router.replace('/(tabs)')}>
          <Text style={styles.backButtonText}>Back to Home</Text>
        </Pressable>
      </View>
    );
  }

  const currentReveal = revealSequence[currentRevealIndex] ?? null;
  const isFinalReveal = currentRevealIndex === revealSequence.length - 1;

  const handleRevealClose = () => {
    if (currentRevealIndex >= revealSequence.length - 1) {
      setRevealComplete(true);
    } else {
      setCurrentRevealIndex((i) => i + 1);
    }
  };

  const handleSkip = () => setRevealComplete(true);

  const isWin = matchup.winnerId === user.userId;
  const isTie = matchup.winnerId === null;

  const userScore = matchup.gameResults
    .filter((r) => r.userId === user.userId)
    .reduce((sum, r) => sum + r.fantasyPoints, 0);
  const cpuScore = matchup.gameResults
    .filter((r) => r.userId !== user.userId)
    .reduce((sum, r) => sum + r.fantasyPoints, 0);

  if (revealComplete) {
    return (
      <View style={styles.container}>
        <ScrollView contentContainerStyle={styles.scoreboardContent}>
          {/* Winner banner */}
          <View style={styles.winnerBanner}>
            <Text style={styles.winnerText}>
              {isTie ? "It's a Tie!" : isWin ? '🏆 You Win!' : 'CPU Wins'}
            </Text>
          </View>

          {/* Scores */}
          <View style={styles.scoreRow}>
            <View style={styles.scoreBlock}>
              <Text style={styles.scoreLabel}>You</Text>
              <Text style={[styles.scoreValue, isWin && styles.scoreValueWin]}>
                {userScore.toFixed(2)}
              </Text>
            </View>
            <Text style={styles.scoreDash}>—</Text>
            <View style={styles.scoreBlock}>
              <Text style={styles.scoreLabel}>CPU</Text>
              <Text style={[styles.scoreValue, !isWin && !isTie && styles.scoreValueWin]}>
                {cpuScore.toFixed(2)}
              </Text>
            </View>
          </View>

          {/* Player summary */}
          <Text style={styles.summaryTitle}>Game Results</Text>
          {revealSequence.map(({ result, isUser }, idx) => {
            const badgeColor = POSITION_COLORS[result.position] ?? '#aaa';
            return (
              <Pressable
                key={idx}
                style={styles.summaryRow}
                onPress={() => setReviewResult({ result, isUser })}
              >
                <View style={styles.summaryLeft}>
                  <Text style={styles.summaryName}>
                    {result.player.firstName} {result.player.lastName}
                  </Text>
                  <Text style={[styles.summaryOwner, isUser ? styles.summaryOwnerUser : styles.summaryOwnerCpu]}>
                    {isUser ? 'You' : 'CPU'}
                  </Text>
                </View>
                <View style={[styles.badge, { backgroundColor: badgeColor + '22', borderColor: badgeColor }]}>
                  <Text style={[styles.badgeText, { color: badgeColor }]}>{result.position}</Text>
                </View>
                <Text style={styles.summaryPts}>{result.fantasyPoints.toFixed(2)} pts</Text>
              </Pressable>
            );
          })}
        </ScrollView>

        <Pressable style={styles.homeButton} onPress={() => router.replace('/(tabs)')}>
          <Text style={styles.homeButtonText}>Back to Home</Text>
        </Pressable>

        {/* Review modal */}
        {reviewResult && (
          <PlayerRevealModal
            visible
            result={reviewResult.result}
            isUserPlayer={reviewResult.isUser}
            onClose={() => setReviewResult(null)}
            isFinal
          />
        )}
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Skip button */}
      <Pressable style={styles.skipButton} onPress={handleSkip}>
        <Text style={styles.skipButtonText}>Skip to Results →</Text>
      </Pressable>

      {/* Reveal modal for current index */}
      <PlayerRevealModal
        visible={!!currentReveal}
        result={currentReveal?.result ?? null}
        isUserPlayer={currentReveal?.isUser ?? true}
        onClose={handleRevealClose}
        isFinal={isFinalReveal}
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
  scoreboardContent: {
    padding: 20,
    paddingBottom: 100,
  },
  winnerBanner: {
    alignItems: 'center',
    paddingVertical: 24,
  },
  winnerText: {
    color: '#FFD700',
    fontSize: 32,
    fontWeight: '900',
    textAlign: 'center',
  },
  scoreRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 16,
    marginBottom: 32,
  },
  scoreBlock: {
    alignItems: 'center',
    minWidth: 100,
  },
  scoreLabel: {
    color: '#888',
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
    marginBottom: 4,
  },
  scoreValue: {
    color: '#ccc',
    fontSize: 36,
    fontWeight: '900',
  },
  scoreValueWin: {
    color: '#FFD700',
  },
  scoreDash: {
    color: '#444',
    fontSize: 28,
    fontWeight: '300',
  },
  summaryTitle: {
    color: '#888',
    fontSize: 12,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 1,
    marginBottom: 12,
  },
  summaryRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#1a1a1a',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#333',
    padding: 14,
    marginBottom: 8,
    gap: 10,
  },
  summaryLeft: {
    flex: 1,
  },
  summaryName: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  summaryOwner: {
    fontSize: 11,
    fontWeight: '600',
    marginTop: 2,
  },
  summaryOwnerUser: {
    color: '#FFD700',
  },
  summaryOwnerCpu: {
    color: '#666',
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
  summaryPts: {
    color: '#FFD700',
    fontSize: 14,
    fontWeight: '700',
    minWidth: 60,
    textAlign: 'right',
  },
  homeButton: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: '#FFD700',
    paddingVertical: 18,
    alignItems: 'center',
  },
  homeButtonText: {
    color: '#000',
    fontSize: 16,
    fontWeight: '700',
  },
  skipButton: {
    position: 'absolute',
    bottom: 40,
    right: 20,
    zIndex: 10,
    backgroundColor: '#1a1a1a',
    borderWidth: 1,
    borderColor: '#444',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  skipButtonText: {
    color: '#ccc',
    fontSize: 13,
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
