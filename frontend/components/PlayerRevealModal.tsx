import { Modal, View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import type { GameResult } from '../services/matchupService';

type Props = {
  visible: boolean;
  result: GameResult | null;
  isUserPlayer: boolean;
  onClose: () => void;
  isFinal?: boolean;
};

const POSITION_COLORS: Record<string, string> = {
  QB: '#4A90D9',
  RB: '#4CAF50',
  WR: '#FF9800',
};

function formatGameDate(dateStr: string): string {
  try {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
  } catch {
    return dateStr;
  }
}

export default function PlayerRevealModal({ visible, result, isUserPlayer, onClose, isFinal }: Props) {
  if (!result) return null;

  const badgeColor = POSITION_COLORS[result.position] ?? '#aaa';

  return (
    <Modal visible={visible} transparent animationType="slide" statusBarTranslucent>
      <View style={styles.overlay}>
        <View style={styles.card}>
          <Text style={[styles.ownerLabel, isUserPlayer ? styles.ownerLabelUser : styles.ownerLabelCpu]}>
            {isUserPlayer ? 'Your Player' : "CPU's Player"}
          </Text>

          <View style={styles.headerRow}>
            <Text style={styles.playerName}>
              {result.player.firstName} {result.player.lastName}
            </Text>
            <View style={[styles.badge, { backgroundColor: badgeColor + '22', borderColor: badgeColor }]}>
              <Text style={[styles.badgeText, { color: badgeColor }]}>{result.position}</Text>
            </View>
          </View>

          <Text style={styles.gameDate}>{formatGameDate(result.gameDate)}</Text>

          <ScrollView style={styles.statsScroll} showsVerticalScrollIndicator={false}>
            {result.position === 'QB' && (
              <>
                {result.passYards !== null && <StatRow label="Pass Yards" value={String(result.passYards)} />}
                {result.passCompletions !== null && result.passAttempts !== null && (
                  <StatRow label="Comp / Att" value={`${result.passCompletions} / ${result.passAttempts}`} />
                )}
                {result.passTds !== null && <StatRow label="Pass TDs" value={String(result.passTds)} />}
                {result.interceptions !== null && <StatRow label="INTs" value={String(result.interceptions)} />}
                {result.passerRating !== null && (
                  <StatRow label="Passer Rating" value={result.passerRating.toFixed(1)} />
                )}
                {result.sacks !== null && <StatRow label="Sacks" value={String(result.sacks)} />}
              </>
            )}

            {result.position === 'RB' && (
              <>
                {result.rushYards !== null && <StatRow label="Rush Yards" value={String(result.rushYards)} />}
                {result.rushAttempts !== null && (
                  <StatRow label="Rush Attempts" value={String(result.rushAttempts)} />
                )}
                {result.rushTds !== null && <StatRow label="Rush TDs" value={String(result.rushTds)} />}
                {result.receptions !== null && <StatRow label="Receptions" value={String(result.receptions)} />}
                {result.recYards !== null && <StatRow label="Rec Yards" value={String(result.recYards)} />}
                {result.recTds !== null && <StatRow label="Rec TDs" value={String(result.recTds)} />}
              </>
            )}

            {result.position === 'WR' && (
              <>
                {result.wrReceptions !== null && (
                  <StatRow label="Receptions" value={String(result.wrReceptions)} />
                )}
                {result.wrYards !== null && <StatRow label="Rec Yards" value={String(result.wrYards)} />}
                {result.wrTds !== null && <StatRow label="Rec TDs" value={String(result.wrTds)} />}
              </>
            )}
          </ScrollView>

          <View style={styles.fantasyRow}>
            <Text style={styles.fantasyPoints}>⭐ {result.fantasyPoints.toFixed(2)} pts</Text>
          </View>

          <Pressable style={styles.closeButton} onPress={onClose}>
            <Text style={styles.closeButtonText}>{isFinal ? 'See Results' : 'Next →'}</Text>
          </Pressable>
        </View>
      </View>
    </Modal>
  );
}

function StatRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.statRow}>
      <Text style={styles.statLabel}>{label}</Text>
      <Text style={styles.statValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'flex-end',
  },
  card: {
    backgroundColor: '#1a1a1a',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 24,
    maxHeight: '80%',
  },
  ownerLabel: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 1,
    textTransform: 'uppercase',
    marginBottom: 8,
  },
  ownerLabelUser: {
    color: '#FFD700',
  },
  ownerLabelCpu: {
    color: '#888',
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginBottom: 4,
  },
  playerName: {
    color: '#fff',
    fontSize: 20,
    fontWeight: '700',
    flex: 1,
  },
  badge: {
    borderWidth: 1,
    borderRadius: 6,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '700',
  },
  gameDate: {
    color: '#666',
    fontSize: 13,
    marginBottom: 16,
  },
  statsScroll: {
    maxHeight: 220,
  },
  statRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#2a2a2a',
  },
  statLabel: {
    color: '#ccc',
    fontSize: 14,
  },
  statValue: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  fantasyRow: {
    alignItems: 'center',
    paddingVertical: 16,
  },
  fantasyPoints: {
    color: '#FFD700',
    fontSize: 28,
    fontWeight: '900',
  },
  closeButton: {
    backgroundColor: '#FFD700',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
  },
  closeButtonText: {
    color: '#000',
    fontSize: 16,
    fontWeight: '700',
  },
});
